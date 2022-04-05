package de.numcodex.feasibility_gui_backend.terminology.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import lombok.Data;

@Entity
@Data
@IdClass(Coding.class)
public class UiProfileContent {
  @Id
  @Column(name = "system")
  private String system;

  @Id
  @Column(name = "code")
  private String code;

  @Id
  @Column(name = "version")
  private String version;

  @Column(name = "query_content", nullable = false)
  private String queryContent;
}
