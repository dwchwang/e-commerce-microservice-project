package com.ecommerce.search.repository;

import com.ecommerce.search.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
}
