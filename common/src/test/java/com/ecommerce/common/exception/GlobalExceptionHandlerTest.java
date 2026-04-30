package com.ecommerce.common.exception;

import com.ecommerce.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingRequestHeaderReturnsBadRequest() {
        MissingRequestHeaderException exception = mock(MissingRequestHeaderException.class);
        when(exception.getHeaderName()).thenReturn("X-User-Id");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingHeader(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Missing required header: X-User-Id");
    }
}
