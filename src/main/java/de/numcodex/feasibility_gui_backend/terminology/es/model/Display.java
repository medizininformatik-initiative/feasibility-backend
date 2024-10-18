package de.numcodex.feasibility_gui_backend.terminology.es.model;

import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Field;

@Builder
public record Display(
    String original,
    @Field(name = "de-DE")
    String deDe,
    @Field(name = "en-US")
    String enUs
) {
}
