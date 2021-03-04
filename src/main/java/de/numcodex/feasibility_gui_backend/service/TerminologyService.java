package de.numcodex.feasibility_gui_backend.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.ui.CategoryEntry;
import de.numcodex.feasibility_gui_backend.model.ui.TerminologyEntry;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TerminologyService {

  public static final String UI_PROFILES_PATH = "src/main/resources/Ui_Profiles";
  private Map<UUID, TerminologyEntry> terminologyEntries = new HashMap<>();
  private List<CategoryEntry> categoryEntries = new ArrayList<>();
  private Map<UUID, TerminologyEntry> terminologyEntriesWithOnlyDirectChildren = new HashMap<>();
  private Map<UUID, Set<TerminologyEntry>> selectableEntriesByCategory = new HashMap<>();

  public TerminologyService() {
    readInTerminologyEntries();
    generateTerminologyEntriesWithoutDirectChildren();
    generateSelectableEntriesByCategory();
  }

  private void readInTerminologyEntries() {
    var files = getFilePathsUiProfiles();

    for (var filename : files) {
      var objectMapper = new ObjectMapper();
      objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      try {
        var terminology_entry = (objectMapper.readValue(
            new URL("file:" + UI_PROFILES_PATH + "/" + filename),
            TerminologyEntry.class));
        terminologyEntries.put(terminology_entry.getId(), terminology_entry);
        categoryEntries.add(new CategoryEntry(terminology_entry.getId(),
            terminology_entry.getDisplay()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void generateTerminologyEntriesWithoutDirectChildren() {
    for (var entry : terminologyEntries.values()) {
      generateTerminologyEntriesWithOnlyDirectChildren(entry);
    }
  }

  private Set<String> getFilePathsUiProfiles() {
    return Stream.of(
        Objects.requireNonNull(new File(UI_PROFILES_PATH).listFiles()))
        .filter(file -> !file.isDirectory())
        .map(File::getName)
        .collect(Collectors.toSet());
  }

  private void generateTerminologyEntriesWithOnlyDirectChildren(TerminologyEntry terminologyTree) {


    var entryWithOnlyDirectChildren = TerminologyEntry.copyWithDirectChildren(terminologyTree);
    terminologyEntriesWithOnlyDirectChildren
        .put(entryWithOnlyDirectChildren.getId(), entryWithOnlyDirectChildren);
    for (var child : terminologyTree.getChildren()) {
      generateTerminologyEntriesWithOnlyDirectChildren(child);
    }
  }

  private Set<TerminologyEntry> getSelectableEntries(TerminologyEntry terminologyEntry) {
    Set<TerminologyEntry> selectableEntries = new HashSet<>();
    if (terminologyEntry.isSelectable()) {
      selectableEntries.add(terminologyEntry);
    }
    for (var child : terminologyEntry.getChildren()) {
      selectableEntries.addAll(getSelectableEntries(child));
    }
    return selectableEntries;
  }

  private void generateSelectableEntriesByCategory() {
    for (var terminologyEntry : terminologyEntries.values()) {
      selectableEntriesByCategory
          .put(terminologyEntry.getId(), getSelectableEntries(terminologyEntry));
    }
  }




  //TODO: Unknown key!
  public TerminologyEntry getEntry(UUID nodeId) {
    return terminologyEntriesWithOnlyDirectChildren.get(nodeId);
  }

  public List<CategoryEntry> getCategories() {
    return categoryEntries;
  }

  public List<TerminologyEntry> getSelectableEntries(String query, UUID categoryId) {
    if (categoryId != null) {
      return selectableEntriesByCategory.get(categoryId).stream()
          .filter((terminologyEntry -> matchesQuery(query, terminologyEntry)))
          .collect(Collectors.toList());
    } else {
      Set<TerminologyEntry> allSelectableEntries = new HashSet<>();
      for (var selectableEntries : selectableEntriesByCategory.values()) {
        allSelectableEntries.addAll(selectableEntries);
      }
      return allSelectableEntries.stream()
          .filter((terminologyEntry -> matchesQuery(query, terminologyEntry)))
          .collect(Collectors.toList());
    }
  }

  private boolean matchesQuery(String query, TerminologyEntry terminologyEntry) {
    return terminologyEntry.getDisplay().toLowerCase().contains(query.toLowerCase()) ||
        (terminologyEntry.getTermCode() != null && terminologyEntry.getTermCode().getCode()
            .contains(query));
  }
}
