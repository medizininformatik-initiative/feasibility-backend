package de.numcodex.feasibility_gui_backend.query.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.sql.Timestamp;
import lombok.Data;

@Data
@Entity
public class SavedQuery {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JoinColumn(referencedColumnName = "id", name = "query_id", nullable = false)
  @OneToOne(fetch = FetchType.LAZY)
  private Query query;

  @Column(name = "deleted")
  private Timestamp deleted;

  @Column(name = "label", nullable = false)
  private String label;

  @Column(name = "comment")
  private String comment;
}
