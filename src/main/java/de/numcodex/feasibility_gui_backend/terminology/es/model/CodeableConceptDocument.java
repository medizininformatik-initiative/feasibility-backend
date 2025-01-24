package de.numcodex.feasibility_gui_backend.terminology.es.model;

import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import jakarta.persistence.Id;
import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Builder
@Document(indexName = "codeable_concept")
public record CodeableConceptDocument(
    @Id String id,
    @Field(name = "termcode")
    TermCode termCode,
    @Field(name = "value_sets")
    List<String> valueSets,
    @Field(name = "display")
    Display display
) {
}
