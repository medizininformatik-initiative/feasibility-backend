package de.numcodex.feasibility_gui_backend.query.persistence;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import lombok.Data;

@Data
@Entity
public class SavedQuery {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(referencedColumnName = "id", name = "query_id")
  private Query query;

  @Column(name = "deleted")
  private Timestamp deleted;

  @Column(name = "label", nullable = false)
  private String label;

  @Column(name = "comment")
  private String comment;
}
