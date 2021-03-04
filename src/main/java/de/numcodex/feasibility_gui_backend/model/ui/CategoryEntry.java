package de.numcodex.feasibility_gui_backend.model.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class CategoryEntry {

  private final UUID catId;
  private final String display;

  public CategoryEntry(@JsonProperty("catId") UUID catId, @JsonProperty("display") String display) {
    this.catId = catId;
    this.display = display;
  }

  public String getDisplay() {
    return display;
  }

  public UUID getCatId() {
    return catId;
  }
}
