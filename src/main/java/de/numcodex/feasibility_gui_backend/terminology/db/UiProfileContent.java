package de.numcodex.feasibility_gui_backend.terminology.db;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
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
