package de.numcodex.feasibility_gui_backend.model.db;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Query {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  private String queryId;

  @ElementCollection
  @Column(columnDefinition="TEXT")
  private Map<String, String> contents = new HashMap<>();
}
