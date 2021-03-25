package de.numcodex.feasibility_gui_backend.model.db;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;
import org.hibernate.annotations.Type;

@Data
@Entity
public class Query {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private String queryId;

  @Type(type = "json")
  private JsonNode structuredQuery;

  @ElementCollection
  @Column(columnDefinition="TEXT")
  private Map<String, String> contents = new HashMap<>();
}
