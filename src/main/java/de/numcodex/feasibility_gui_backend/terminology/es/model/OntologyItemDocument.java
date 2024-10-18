package de.numcodex.feasibility_gui_backend.terminology.es.model;

import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import jakarta.persistence.Id;
import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Collection;

@Builder
@Document(indexName = "ontology")
public record OntologyItemDocument(
    @Id String id,
    int availability,
    TermCode context,
    String terminology,
    String termcode,
    @Field(name = "display")
    Display display,
    @Field(name = "kds_module")
    String kdsModule,
    @Field(name = "parents")
    Collection<Relative> parents,
    @Field(name = "children")
    Collection<Relative> children,
    @Field(name = "related_terms")
    Collection<Relative> relatedTerms
) {
}
