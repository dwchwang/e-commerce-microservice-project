package com.ecommerce.content.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.content.dto.BlogPostRequest;
import com.ecommerce.content.dto.BlogPostResponse;
import com.ecommerce.content.entity.BlogPost;
import com.ecommerce.content.repository.BlogPostRepository;
import com.ecommerce.content.service.BlogPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlogPostServiceImpl implements BlogPostService {

    private final BlogPostRepository blogPostRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BlogPostResponse> getPublishedPosts(Pageable pageable) {
        return blogPostRepository.findByIsPublishedTrue(pageable).map(BlogPostResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPostResponse getPublishedPost(String slug) {
        return BlogPostResponse.from(blogPostRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug)));
    }

    @Override
    @Transactional
    public BlogPostResponse createPost(BlogPostRequest request) {
        String slug = resolveSlug(request.getSlug(), request.getTitle());
        if (blogPostRepository.existsBySlug(slug)) {
            throw new BusinessException("Slug already exists");
        }
        boolean publish = Boolean.TRUE.equals(request.getIsPublished());
        BlogPost post = BlogPost.builder()
                .title(request.getTitle())
                .slug(slug)
                .content(request.getContent())
                .thumbnailUrl(request.getThumbnailUrl())
                .author(request.getAuthor())
                .isPublished(publish)
                .publishedAt(publish ? Instant.now() : null)
                .build();
        return BlogPostResponse.from(blogPostRepository.save(post));
    }

    @Override
    @Transactional
    public BlogPostResponse updatePost(UUID id, BlogPostRequest request) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        String slug = resolveSlug(request.getSlug(), request.getTitle());
        if (blogPostRepository.existsBySlugAndIdNot(slug, id)) {
            throw new BusinessException("Slug already exists");
        }
        boolean wasPublished = Boolean.TRUE.equals(post.getIsPublished());
        boolean publish = request.getIsPublished() != null ? request.getIsPublished() : wasPublished;
        post.setTitle(request.getTitle());
        post.setSlug(slug);
        post.setContent(request.getContent());
        post.setThumbnailUrl(request.getThumbnailUrl());
        post.setAuthor(request.getAuthor());
        post.setIsPublished(publish);
        if (publish && !wasPublished) {
            post.setPublishedAt(Instant.now());
        }
        if (!publish) {
            post.setPublishedAt(null);
        }
        return BlogPostResponse.from(blogPostRepository.save(post));
    }

    @Override
    @Transactional
    public BlogPostResponse publishPost(UUID id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        post.setIsPublished(true);
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }
        return BlogPostResponse.from(blogPostRepository.save(post));
    }

    @Override
    @Transactional
    public BlogPostResponse unpublishPost(UUID id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        post.setIsPublished(false);
        post.setPublishedAt(null);
        return BlogPostResponse.from(blogPostRepository.save(post));
    }

    @Override
    @Transactional
    public void deletePost(UUID id) {
        if (!blogPostRepository.existsById(id)) {
            throw new ResourceNotFoundException("Post", "id", id);
        }
        blogPostRepository.deleteById(id);
    }

    String resolveSlug(String requestedSlug, String title) {
        String source = StringUtils.hasText(requestedSlug) ? requestedSlug : title;
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("Slug is required");
        }
        return normalized;
    }
}
