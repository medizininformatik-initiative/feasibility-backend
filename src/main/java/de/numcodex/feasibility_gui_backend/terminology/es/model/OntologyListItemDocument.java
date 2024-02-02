package de.numcodex.feasibility_gui_backend.terminology.es.model;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Builder
@Document(indexName = "ontology")
public class OntologyListItemDocument {

    private @Id String id;
    private String name;
    private int availability;
    private String context;
    private String terminology;
    private String termcode;
    private String kdsModule;
}
