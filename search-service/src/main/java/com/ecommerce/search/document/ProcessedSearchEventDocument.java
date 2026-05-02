package com.ecommerce.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "search_processed_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedSearchEventDocument {

    @Id
    private String eventId;

    @Field(type = FieldType.Keyword)
    private String topic;

    @Field(type = FieldType.Date)
    private Instant processedAt;
}
