package de.numcodex.feasibility_gui_backend.model.db;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

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
  private Map<String, String> contents = new HashMap<>();
}
