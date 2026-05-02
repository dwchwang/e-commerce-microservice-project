package com.ecommerce.content.service;

import com.ecommerce.content.dto.BlogPostRequest;
import com.ecommerce.content.dto.BlogPostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BlogPostService {

    Page<BlogPostResponse> getPublishedPosts(Pageable pageable);

    BlogPostResponse getPublishedPost(String slug);

    BlogPostResponse createPost(BlogPostRequest request);

    BlogPostResponse updatePost(UUID id, BlogPostRequest request);

    BlogPostResponse publishPost(UUID id);

    BlogPostResponse unpublishPost(UUID id);

    void deletePost(UUID id);
}
