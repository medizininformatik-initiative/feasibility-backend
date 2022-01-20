package de.numcodex.feasibility_gui_backend.terminology.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.numcodex.feasibility_gui_backend.common.api.TermCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TerminologyEntry {

    @JsonProperty("children")
    private List<TerminologyEntry> children = new ArrayList<>();
    @JsonProperty("termCode")
    private TermCode termCode;
    @JsonProperty("termCodes")
    @EqualsAndHashCode.Include
    private List<TermCode> termCodes = new ArrayList<>();
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("leaf")
    private boolean leaf;
    @JsonProperty("selectable")
    private boolean selectable;
    @JsonProperty("timeRestrictionAllowed")
    private boolean timeRestrictionAllowed;
    @JsonProperty("valueDefinition")
    private ValueDefinition valueDefinition;
    @JsonProperty("valueDefinitions")
    private List<ValueDefinition> valueDefinitions = new ArrayList<>();
    @JsonProperty("attributeDefinitions")
    private List<AttributeDefinition> attributeDefinitions = new ArrayList<>();
    @JsonProperty("display")
    private String display;

    public static TerminologyEntry copyWithDirectChildren(TerminologyEntry other) {
        var terminology_entry = copyWithoutChildren(other);
        terminology_entry.children = other.children.stream().map(TerminologyEntry::copyWithoutChildren)
                .collect(
                        Collectors.toList());
        return terminology_entry;
    }

    public static TerminologyEntry copyWithoutChildren(TerminologyEntry other) {
        var terminologyEntry = new TerminologyEntry();
        terminologyEntry.termCode = other.termCode;
        terminologyEntry.termCodes = other.termCodes;
        terminologyEntry.id = other.id;
        terminologyEntry.leaf = other.leaf;
        terminologyEntry.selectable = other.selectable;
        terminologyEntry.timeRestrictionAllowed = other.timeRestrictionAllowed;
        terminologyEntry.valueDefinition = other.valueDefinition;
        terminologyEntry.valueDefinitions = other.valueDefinitions;
        terminologyEntry.attributeDefinitions = other.attributeDefinitions;
        terminologyEntry.display = other.display;
        return terminologyEntry;
    }
}
