package de.numcodex.feasibility_gui_backend.terminology.es.model;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Collection;

@Data
@Builder
@Document(indexName = "ontology")
public class OntologyItemDocument {

    private @Id String id;
    private String name;
    private int availability;
    private String context;
    private String terminology;
    private String termcode;
    @Field(name = "kds_module") private String kdsModule;

    @Field(type = FieldType.Nested, includeInParent = true, name = "translations")
    private Collection<Translation> translations;
    @Field(type = FieldType.Nested, includeInParent = true, name = "parents")
    private Collection<Relative> parents;
    @Field(type = FieldType.Nested, includeInParent = true, name = "children")
    private Collection<Relative> children;
    @Field(type = FieldType.Nested, includeInParent = true, name = "related_terms")
    private Collection<Relative> relatedTerms;
}
