package com.ecommerce.identity.service.impl;

import com.ecommerce.common.event.UserRegisteredEvent;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.identity.config.KeycloakConfig;
import com.ecommerce.identity.dto.AssignRoleRequest;
import com.ecommerce.identity.dto.LoginRequest;
import com.ecommerce.identity.dto.RefreshTokenRequest;
import com.ecommerce.identity.dto.RegisterRequest;
import com.ecommerce.identity.dto.TokenResponse;
import com.ecommerce.identity.kafka.UserRegisteredProducer;
import com.ecommerce.identity.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final Keycloak keycloak;
    private final KeycloakConfig keycloakConfig;
    private final RestTemplate restTemplate;
    private final UserRegisteredProducer userRegisteredProducer;

    @Override
    public String register(RegisterRequest request) {
        UserRepresentation user = new UserRepresentation();
        NameParts nameParts = splitFullName(request.getFullName());
        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setFirstName(nameParts.firstName());
        user.setLastName(nameParts.lastName());
        user.setEnabled(true);
        user.setEmailVerified(true);

        try (Response response = keycloak.realm(keycloakConfig.getRealm()).users().create(user)) {
            int status = response.getStatus();
            if (status == Response.Status.CONFLICT.getStatusCode()) {
                throw new BusinessException("Email already exists");
            }
            if (status != Response.Status.CREATED.getStatusCode()) {
                throw new BusinessException("Cannot create user in Keycloak");
            }

            String keycloakUserId = CreatedResponseUtil.getCreatedId(response);
            keycloak.realm(keycloakConfig.getRealm())
                    .users()
                    .get(keycloakUserId)
                    .resetPassword(passwordCredential(request.getPassword()));
            assignRealmRole(keycloakUserId, DEFAULT_ROLE);
            publishUserRegistered(keycloakUserId, request);
            return keycloakUserId;
        }
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("grant_type", "password");
        form.add("username", request.getEmail());
        form.add("password", request.getPassword());
        return requestToken(form);
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", request.getRefreshToken());
        return requestToken(form);
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("refresh_token", request.getRefreshToken());
        postForm(logoutUrl(), form);
    }

    @Override
    public void assignRole(AssignRoleRequest request) {
        assignRealmRole(request.getUserId(), request.getRoleName());
    }

    private CredentialRepresentation passwordCredential(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        return credential;
    }

    private NameParts splitFullName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        int separator = normalized.lastIndexOf(' ');
        if (separator < 1) {
            return new NameParts(normalized, normalized);
        }
        return new NameParts(normalized.substring(0, separator), normalized.substring(separator + 1));
    }

    private record NameParts(String firstName, String lastName) {
    }

    private void assignRealmRole(String userId, String roleName) {
        RoleRepresentation role = keycloak.realm(keycloakConfig.getRealm())
                .roles()
                .get(roleName)
                .toRepresentation();
        keycloak.realm(keycloakConfig.getRealm())
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(List.of(role));
    }

    private void publishUserRegistered(String keycloakUserId, RegisterRequest request) {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(keycloakUserId)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .timestamp(LocalDateTime.now())
                .build();
        userRegisteredProducer.send(event);
    }

    private MultiValueMap<String, String> baseClientForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakConfig.getClientId());
        form.add("client_secret", keycloakConfig.getClientSecret());
        return form;
    }

    private TokenResponse requestToken(MultiValueMap<String, String> form) {
        ResponseEntity<JsonNode> response = postForm(tokenUrl(), form);
        JsonNode body = response.getBody();
        if (body == null) {
            throw new BusinessException("Empty token response from Keycloak");
        }
        return TokenResponse.builder()
                .accessToken(body.path("access_token").asText())
                .refreshToken(body.path("refresh_token").asText())
                .expiresIn(body.path("expires_in").asLong())
                .tokenType(body.path("token_type").asText())
                .build();
    }

    private ResponseEntity<JsonNode> postForm(String url, MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            return restTemplate.postForEntity(url, new HttpEntity<>(form, headers), JsonNode.class);
        } catch (HttpStatusCodeException ex) {
            String message = extractErrorMessage(ex);
            throw new BusinessException(message);
        }
    }

    private String extractErrorMessage(HttpStatusCodeException ex) {
        Map<String, String> fallback = Map.of(
                "invalid_grant", "Invalid credentials or refresh token",
                "invalid_client", "Invalid Keycloak client configuration"
        );
        String body = ex.getResponseBodyAsString();
        return fallback.entrySet().stream()
                .filter(entry -> body.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("Keycloak request failed");
    }

    private String tokenUrl() {
        return keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm()
                + "/protocol/openid-connect/token";
    }

    private String logoutUrl() {
        return keycloakConfig.getServerUrl() + "/realms/" + keycloakConfig.getRealm()
                + "/protocol/openid-connect/logout";
    }
}
