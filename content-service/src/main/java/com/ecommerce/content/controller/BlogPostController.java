package com.ecommerce.content.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.content.dto.BlogPostRequest;
import com.ecommerce.content.dto.BlogPostResponse;
import com.ecommerce.content.service.BlogPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/content/posts")
@RequiredArgsConstructor
public class BlogPostController {

    private final BlogPostService blogPostService;

    @GetMapping
    public ApiResponse<Page<BlogPostResponse>> getPublishedPosts(Pageable pageable) {
        return ApiResponse.ok(blogPostService.getPublishedPosts(pageable));
    }

    @GetMapping("/{slug}")
    public ApiResponse<BlogPostResponse> getPublishedPost(@PathVariable String slug) {
        return ApiResponse.ok(blogPostService.getPublishedPost(slug));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BlogPostResponse> createPost(@Valid @RequestBody BlogPostRequest request) {
        return ApiResponse.ok("Post created successfully", blogPostService.createPost(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BlogPostResponse> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody BlogPostRequest request) {
        return ApiResponse.ok(blogPostService.updatePost(id, request));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BlogPostResponse> publishPost(@PathVariable UUID id) {
        return ApiResponse.ok(blogPostService.publishPost(id));
    }

    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BlogPostResponse> unpublishPost(@PathVariable UUID id) {
        return ApiResponse.ok(blogPostService.unpublishPost(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<Void> deletePost(@PathVariable UUID id) {
        blogPostService.deletePost(id);
        return ApiResponse.ok("Post deleted successfully", null);
    }
}
