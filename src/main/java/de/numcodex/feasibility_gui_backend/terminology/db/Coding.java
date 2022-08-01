package de.numcodex.feasibility_gui_backend.terminology.db;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;

public class Coding implements Serializable {

  private String system;
  private String code;
  private String version;

  public Coding() {
  }

  public Coding(String system, String code, String version) {
    requireNonNull(system);
    requireNonNull(code);
    this.system = system;
    this.code = code;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Coding)) {
      return false;
    }
    Coding coding = (Coding) o;
    return (this.version.equals(coding.version) && this.code.equals(coding.code) &&
        this.system.equals(coding.system));
  }

  @Override
  public int hashCode() {
    return Objects.hash(system, code, version);
  }
}
