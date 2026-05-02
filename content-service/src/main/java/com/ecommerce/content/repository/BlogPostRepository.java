package com.ecommerce.content.repository;

import com.ecommerce.content.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogPostRepository extends JpaRepository<BlogPost, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Page<BlogPost> findByIsPublishedTrue(Pageable pageable);

    Optional<BlogPost> findBySlugAndIsPublishedTrue(String slug);
}
