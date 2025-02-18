package de.numcodex.feasibility_gui_backend.query.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Dataquery {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_by", updatable = false)
  private String createdBy;

  @Column(name = "label", nullable = false)
  private String label;

  @Column(name = "comment")
  private String comment;

  @Column(name = "crtdl")
  private String crtdl;

  @Column(name = "last_modified", insertable = false)
  private Timestamp lastModified;

  @Column(name = "result_size")
  private Long resultSize;

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Dataquery dataquery = (Dataquery) o;
    return Objects.equals(id, dataquery.id) && Objects.equals(createdBy, dataquery.createdBy) && Objects.equals(label, dataquery.label) && Objects.equals(comment, dataquery.comment) && Objects.equals(crtdl, dataquery.crtdl) && Objects.equals(lastModified, dataquery.lastModified) && Objects.equals(resultSize, dataquery.resultSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, createdBy, label, comment, crtdl, lastModified, resultSize);
  }
}
