package com.ecommerce.content.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.content.dto.BlogPostRequest;
import com.ecommerce.content.entity.BlogPost;
import com.ecommerce.content.repository.BlogPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogPostServiceImplTest {

    @Mock
    private BlogPostRepository blogPostRepository;

    private BlogPostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BlogPostServiceImpl(blogPostRepository);
    }

    @Test
    void generatesSlugFromTitle() {
        assertThat(service.resolveSlug(null, "iPhone 15 Pro Max Review"))
                .isEqualTo("iphone-15-pro-max-review");
    }

    @Test
    void duplicateSlugRejectedOnCreate() {
        BlogPostRequest request = request();
        when(blogPostRepository.existsBySlug("hello-world")).thenReturn(true);

        assertThatThrownBy(() -> service.createPost(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Slug already exists");
    }

    @Test
    void createPublishedPostSetsPublishedAt() {
        BlogPostRequest request = request();
        request.setIsPublished(true);
        when(blogPostRepository.existsBySlug("hello-world")).thenReturn(false);
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.createPost(request).getPublishedAt()).isNotNull();
    }

    @Test
    void unpublishClearsPublishedAt() {
        UUID id = UUID.randomUUID();
        BlogPost post = BlogPost.builder()
                .id(id)
                .title("Hello")
                .slug("hello")
                .content("Body")
                .isPublished(true)
                .publishedAt(java.time.Instant.now())
                .build();
        when(blogPostRepository.findById(id)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(post)).thenReturn(post);

        service.unpublishPost(id);

        assertThat(post.getIsPublished()).isFalse();
        assertThat(post.getPublishedAt()).isNull();
        verify(blogPostRepository).save(post);
    }

    private BlogPostRequest request() {
        BlogPostRequest request = new BlogPostRequest();
        request.setTitle("Hello World");
        request.setContent("Body");
        return request;
    }
}
