package com.ecommerce.search.repository;

import com.ecommerce.search.document.ProcessedSearchEventDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProcessedSearchEventRepository extends ElasticsearchRepository<ProcessedSearchEventDocument, String> {
}
