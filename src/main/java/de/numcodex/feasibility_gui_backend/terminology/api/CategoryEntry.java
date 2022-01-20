package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

@JsonInclude(Include.NON_NULL)
public class CategoryEntry {

    private final UUID catId;
    private final String display;

    public CategoryEntry(@JsonProperty("catId") UUID catId, @JsonProperty("display") String display) {
        this.catId = Objects.requireNonNull(catId);
        this.display = Objects.requireNonNull(display);
    }

    public String getDisplay() {
        return display;
    }

    public UUID getCatId() {
        return catId;
    }
}
