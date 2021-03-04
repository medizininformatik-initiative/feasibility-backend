package de.numcodex.feasibility_gui_backend.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.model.ui.CategoryEntry;
import de.numcodex.feasibility_gui_backend.model.ui.TerminologyEntry;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.File;
import org.springframework.stereotype.Service;

@Service
public class TerminologyService {
  private HashMap<UUID, TerminologyEntry> terminologyEntries  = new HashMap<>();
  private List<CategoryEntry> categoryEntries = new ArrayList<>();
  private HashMap<UUID, TerminologyEntry> terminologyEntriesWithOnlyDirectChildren= new HashMap<>();

  public TerminologyService() {
    readInTerminologyEntries();
    for(var entry : terminologyEntries.values()){
      generateTerminologyEntriesWithOnlyDirectChildren(entry);
    }
  }

  private void readInTerminologyEntries()
  {
    var files = Stream.of(
        Objects.requireNonNull(new File("src/main/resources/Ui_Profiles").listFiles()))
        .filter(file -> !file.isDirectory())
        .map(File::getName)
        .collect(Collectors.toSet());

    for (var filename : files) {
      var objectMapper = new ObjectMapper();
      objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      try {
        var terminology_entry = (objectMapper.readValue(
            new URL("file:src/main/resources/Ui_Profiles/" + filename),
            TerminologyEntry.class));
        terminologyEntries.put(terminology_entry.getId(), terminology_entry);
        categoryEntries.add(new CategoryEntry(terminology_entry.getId(),
            terminology_entry.getDisplay()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void generateTerminologyEntriesWithOnlyDirectChildren(TerminologyEntry terminologyTree)
  {
    var entryWithoutChildren = new TerminologyEntry();
    entryWithoutChildren.copy(terminologyTree);
    terminologyEntriesWithOnlyDirectChildren.put(entryWithoutChildren.getId(),entryWithoutChildren);
    for (var child : terminologyTree.getChildren())
    {
      generateTerminologyEntriesWithOnlyDirectChildren(child);
    }
  }

  //TODO: Unknown key!
  public TerminologyEntry getEntry(UUID nodeId) {
    return terminologyEntriesWithOnlyDirectChildren.get(nodeId);
  }

  public List<CategoryEntry> getCategories() {
      return categoryEntries;
  }

  public List<TerminologyEntry> queryEntries(String query) {
    List<TerminologyEntry> result = new ArrayList<>();
    for(var entry : terminologyEntriesWithOnlyDirectChildren.values())
    {
      if(entry.getDisplay().startsWith(query))
      {
        result.add(entry);
      }
    }
    return result;
  }

  public List<TerminologyEntry> getValueSet(String ValueSet) {
    return null;
  }
}
