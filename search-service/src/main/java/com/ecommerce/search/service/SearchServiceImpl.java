package com.ecommerce.search.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.search.document.ProductDocument;
import com.ecommerce.search.dto.SearchRequest;
import com.ecommerce.search.dto.SearchResponse;
import com.ecommerce.search.dto.SuggestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final List<String> ALLOWED_SORTS = List.of("relevance", "price_asc", "price_desc", "newest");

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<SearchResponse> search(SearchRequest request) {
        SearchRequest normalized = normalize(request);
        Pageable pageable = PageRequest.of(normalized.getPage(), normalized.getSize(), sort(normalized.getSortBy()));
        Criteria criteria = Criteria.where("isActive").is(true);

        if (StringUtils.hasText(normalized.getQ())) {
            Criteria textCriteria = Criteria.where("name").contains(normalized.getQ())
                    .or("brandName").contains(normalized.getQ())
                    .or("description").contains(normalized.getQ())
                    .or("categoryName").contains(normalized.getQ());
            criteria = criteria.and(textCriteria);
        }
        if (StringUtils.hasText(normalized.getCategoryId())) {
            criteria = criteria.and("categoryId").is(normalized.getCategoryId());
        }
        if (StringUtils.hasText(normalized.getBrandId())) {
            criteria = criteria.and("brandId").is(normalized.getBrandId());
        }
        if (normalized.getMinPrice() != null) {
            criteria = criteria.and("price").greaterThanEqual(normalized.getMinPrice());
        }
        if (normalized.getMaxPrice() != null) {
            criteria = criteria.and("price").lessThanEqual(normalized.getMaxPrice());
        }

        CriteriaQuery query = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        List<SearchResponse> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(SearchResponse::from)
                .toList();
        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }

    @Override
    public SuggestionResponse suggestions(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new SuggestionResponse(List.of());
        }
        SearchRequest request = SearchRequest.builder()
                .q(keyword)
                .page(0)
                .size(10)
                .sortBy("relevance")
                .build();
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        search(request).getContent().stream()
                .map(SearchResponse::getName)
                .filter(StringUtils::hasText)
                .forEach(suggestions::add);
        return new SuggestionResponse(List.copyOf(suggestions));
    }

    private SearchRequest normalize(SearchRequest request) {
        SearchRequest source = request != null ? request : new SearchRequest();
        int page = source.getPage() != null && source.getPage() >= 0 ? source.getPage() : 0;
        int size = source.getSize() != null ? Math.min(Math.max(source.getSize(), 1), 100) : 20;
        String sortBy = StringUtils.hasText(source.getSortBy()) ? source.getSortBy() : "relevance";
        if (!ALLOWED_SORTS.contains(sortBy)) {
            throw new BusinessException("Invalid sortBy");
        }
        return SearchRequest.builder()
                .q(source.getQ())
                .categoryId(source.getCategoryId())
                .brandId(source.getBrandId())
                .minPrice(source.getMinPrice())
                .maxPrice(source.getMaxPrice())
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .build();
    }

    private Sort sort(String sortBy) {
        return switch (sortBy) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.unsorted();
        };
    }
}
