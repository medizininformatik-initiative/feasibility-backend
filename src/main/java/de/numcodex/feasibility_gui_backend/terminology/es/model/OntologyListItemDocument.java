package de.numcodex.feasibility_gui_backend.terminology.es.model;

import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

@Data
@Builder
@Document(indexName = "ontology")
public class OntologyListItemDocument {

    private @Id String id;
    private String name;
    private int availability;
    private TermCode context;
    private String terminology;
    private String termcode;
    @Field(name = "kds_module")
    private String kdsModule;
}
