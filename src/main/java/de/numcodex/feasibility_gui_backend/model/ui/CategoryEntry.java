package de.numcodex.feasibility_gui_backend.model.ui;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class CategoryEntry {
  private final UUID entryId;
  private final String display;

  public CategoryEntry(@JsonProperty("id") UUID entryId, @JsonProperty("display") String display) {
    this.entryId = entryId;
    this.display = display;
  }

  public String getDisplay() {
    return display;
  }

  public UUID getEntryId() {
    return entryId;
  }
}
