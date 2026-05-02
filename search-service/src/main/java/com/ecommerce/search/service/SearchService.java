package com.ecommerce.search.service;

import com.ecommerce.search.dto.SearchRequest;
import com.ecommerce.search.dto.SearchResponse;
import com.ecommerce.search.dto.SuggestionResponse;
import org.springframework.data.domain.Page;

public interface SearchService {

    Page<SearchResponse> search(SearchRequest request);

    SuggestionResponse suggestions(String keyword);
}
