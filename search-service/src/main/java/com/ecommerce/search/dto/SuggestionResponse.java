package com.ecommerce.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SuggestionResponse {

    private List<String> suggestions;
}
