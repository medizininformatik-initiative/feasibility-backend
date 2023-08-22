package de.numcodex.feasibility_gui_backend.terminology.persistence;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Entity(name = "UI_PROFILE")
@Table(name = "UI_PROFILE_TABLE")
@Data
@IdClass(Coding.class)
public class UiProfileContent implements Serializable {
  @Id
  @Column(name = "system")
  private String system;

  @Id
  @Column(name = "code")
  private String code;

  @Id
  @Column(name = "version")
  private String version;

  @Column(name = "UI_Profile", nullable = false)
  private String uiProfile;
}
