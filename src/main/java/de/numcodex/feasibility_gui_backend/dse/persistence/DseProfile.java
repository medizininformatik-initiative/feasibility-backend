package de.numcodex.feasibility_gui_backend.dse.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Entity
@Table(name = "dse_profile", schema = "public")
@Data
@EqualsAndHashCode
public class DseProfile implements Serializable {
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  @Column(name = "id", nullable = false)
  private int id;
  @Basic
  @Column(name = "url", nullable = false, length = -1)
  private String url;
  @Basic
  @Column(name = "entry", columnDefinition = "json", nullable = false)
  private String entry;
}
