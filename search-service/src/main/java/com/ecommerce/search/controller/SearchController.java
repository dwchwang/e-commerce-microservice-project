package com.ecommerce.search.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.search.dto.SearchRequest;
import com.ecommerce.search.dto.SearchResponse;
import com.ecommerce.search.dto.SuggestionResponse;
import com.ecommerce.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ApiResponse<Page<SearchResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "relevance") String sortBy) {
        return ApiResponse.ok(searchService.search(SearchRequest.builder()
                .q(q)
                .categoryId(categoryId)
                .brandId(brandId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .build()));
    }

    @GetMapping("/suggestions")
    public ApiResponse<SuggestionResponse> suggestions(@RequestParam(required = false) String q) {
        return ApiResponse.ok(searchService.suggestions(q));
    }
}
