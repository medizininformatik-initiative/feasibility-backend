package de.numcodex.feasibility_gui_backend.terminology;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.terminology.api.CategoryEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.CriteriaProfileData;
import de.numcodex.feasibility_gui_backend.terminology.api.TerminologyEntry;
import de.numcodex.feasibility_gui_backend.terminology.persistence.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class TerminologyService {

  private final UiProfileRepository uiProfileRepository;

  private final TermCodeRepository termCodeRepository;

  private final ContextualizedTermCodeRepository contextualizedTermCodeRepository;

  private final MappingRepository mappingRepository;

  private String uiProfilePath;

  @NonNull
  private ObjectMapper jsonUtil;

  @Value("${app.ontologyOrder}")
  private List<String> sortedCategories;
  private Map<UUID, TerminologyEntry> terminologyEntries = new HashMap<>();
  private List<CategoryEntry> categoryEntries = new ArrayList<>();
  private Map<UUID, TerminologyEntry> terminologyEntriesWithOnlyDirectChildren = new HashMap<>();
  private Map<UUID, Set<TerminologyEntry>> selectableEntriesByCategory = new HashMap<>();

  public TerminologyService(@Value("${app.ontologyFolder}") String uiProfilePath,
                            UiProfileRepository uiProfileRepository,
                            TermCodeRepository termCodeRepository,
                            ContextualizedTermCodeRepository contextualizedTermCodeRepository,
                            MappingRepository mappingRepository,
                            ObjectMapper jsonUtil) throws IOException {
    this.uiProfilePath = uiProfilePath;
    readInTerminologyEntries();
    generateTerminologyEntriesWithoutDirectChildren();
    generateSelectableEntriesByCategory();
    this.uiProfileRepository = uiProfileRepository;
    this.termCodeRepository = termCodeRepository;
    this.contextualizedTermCodeRepository = contextualizedTermCodeRepository;
    this.mappingRepository = mappingRepository;
    this.jsonUtil = jsonUtil;
  }

  private void readInTerminologyEntries() throws IOException {
    var files = getFilePathsUiProfiles();

    for (var filename : files) {
      if (!filename.toLowerCase().endsWith(".json")) {
        log.trace("Skipping non-json file: {}", filename);
        continue;
      }
      var objectMapper = new ObjectMapper();
      objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      try {
        var terminology_entry = (objectMapper.readValue(
            new URL("file:" + uiProfilePath + "/" + filename),
            TerminologyEntry.class));
        terminologyEntries.put(terminology_entry.getId(), terminology_entry);
        categoryEntries.add(new CategoryEntry(terminology_entry.getId(),
            terminology_entry.getDisplay()));
      } catch (IOException e) {
        throw new IOException("Could not read terminology files", e);
      }
    }
  }

  private void generateTerminologyEntriesWithoutDirectChildren() {
    for (var entry : terminologyEntries.values()) {
      generateTerminologyEntriesWithOnlyDirectChildren(entry);
    }
  }

  private Set<String> getFilePathsUiProfiles() {
    log.info("Ui Profile Path -> {}", uiProfilePath);

    return Stream.of(
            Objects.requireNonNull(new File(uiProfilePath).listFiles()))
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
      var terminologyEntryWithoutChildren = TerminologyEntry.copyWithoutChildren(terminologyEntry);
      selectableEntries.add(terminologyEntryWithoutChildren);
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

  public TerminologyEntry getEntry(UUID nodeId) throws NodeNotFoundException {
    TerminologyEntry terminologyEntry = terminologyEntriesWithOnlyDirectChildren.get(nodeId);
    if (terminologyEntry == null) {
      throw new NodeNotFoundException();
    }
    return terminologyEntry;
  }

  public List<CategoryEntry> getCategories() {
    var sortedCategoryEntries = new ArrayList<CategoryEntry>();

    List<CategoryEntry> found = categoryEntries.stream().filter(value -> sortedCategories.contains(value.getDisplay())).collect(Collectors.toList());
    List<CategoryEntry> notFound = categoryEntries.stream().filter(value -> !sortedCategories.contains(value.getDisplay())).collect(Collectors.toList());

    found.sort(Comparator.comparing(value -> sortedCategories.indexOf(value.getDisplay())));
    notFound.sort(Comparator.comparing(CategoryEntry::getDisplay));

    sortedCategoryEntries.addAll(found);
    sortedCategoryEntries.addAll(notFound);

    return sortedCategoryEntries;
  }

  public List<TerminologyEntry> getSelectableEntries(String query, UUID categoryId) {
    if (categoryId != null) {
      return selectableEntriesByCategory.get(categoryId).stream()
          .filter((terminologyEntry -> matchesQuery(query, terminologyEntry))).sorted((Comparator
              .comparingInt(val -> val.getDisplay().length()))).limit(20)
          .collect(Collectors.toList());
    } else {
      Set<TerminologyEntry> allSelectableEntries = new HashSet<>();
      for (var selectableEntries : selectableEntriesByCategory.values()) {
        allSelectableEntries.addAll(selectableEntries);
      }
      log.debug("Selectable entries: {}", allSelectableEntries.size());
      return allSelectableEntries.stream()
          .filter((terminologyEntry -> matchesQuery(query, terminologyEntry))).sorted((Comparator
              .comparingInt(val -> val.getDisplay().length()))).limit(20)
          .collect(Collectors.toList());
    }
  }

  public String getUiProfile(String contextualizedTermCodeHash)
          throws UiProfileNotFoundException {
    Optional<UiProfile> uiProfile = uiProfileRepository.findByContextualizedTermcodeHash(contextualizedTermCodeHash);
    if (uiProfile.isPresent()) {
      return uiProfile.get().getUiProfile();
    } else {
      throw new UiProfileNotFoundException();
    }
  }

  public String getMapping(String contextualizedTermCodeHash)
          throws MappingNotFoundException {
    Optional<Mapping> mapping = mappingRepository.findByContextualizedTermcodeHash(contextualizedTermCodeHash);
    if (mapping.isPresent()) {
      return mapping.get().getContent();
    } else {
      throw new MappingNotFoundException();
    }
  }

  public boolean isExistingTermCode(String system, String code, String version) {
    return (version == null) ? termCodeRepository.existsTermCode(system, code) : termCodeRepository.existsTermCode(system, code, version);
  }

  public static int min(int... numbers) {
    return Arrays.stream(numbers)
        .min().orElse(Integer.MAX_VALUE);
  }

  private boolean matchesQuery(String query, TerminologyEntry terminologyEntry) {
    return terminologyEntry.getDisplay().toLowerCase().startsWith(query.toLowerCase()) ||
        Arrays.stream(terminologyEntry.getDisplay().toLowerCase().split(" "))
            .anyMatch(var -> var.startsWith(query.toLowerCase())) ||
        (terminologyEntry.getTermCodes().stream().anyMatch(termCode -> termCode.code().toLowerCase()
            .startsWith(query.toLowerCase())));
  }

  public List<String> getIntersection(String criteriaSetUrl, List<String> contextTermCodeHashList) {
    return contextualizedTermCodeRepository.filterByCriteriaSetUrl(criteriaSetUrl, contextTermCodeHashList);
  }

  public List<CriteriaProfileData> getCriteriaProfileData(List<String> criteriaIds) {
    List<CriteriaProfileData> results = new ArrayList<>();

    for (String id : criteriaIds) {
      CriteriaProfileData criteriaProfileData = new CriteriaProfileData();
      TermCode tc = termCodeRepository.findTermCodeByContextualizedTermcodeHash(id).orElse(null);
      Context c = termCodeRepository.findContextByContextualizedTermcodeHash(id).orElse(null);
      criteriaProfileData.setId(id);
      try {
        de.numcodex.feasibility_gui_backend.terminology.api.UiProfile uip = jsonUtil.readValue(getUiProfile(id), de.numcodex.feasibility_gui_backend.terminology.api.UiProfile.class);
        criteriaProfileData.setUiProfile(uip);
      } catch (UiProfileNotFoundException | JsonProcessingException e) {
        criteriaProfileData.setUiProfile(null);
      }
      if (c != null) {
        criteriaProfileData.setContext(de.numcodex.feasibility_gui_backend.common.api.TermCode.builder()
            .code(c.getCode())
            .display(c.getDisplay())
            .system(c.getSystem())
            .version(c.getVersion())
            .build());
      } else {
        criteriaProfileData.setContext(null);
      }
      if (tc != null) {
        criteriaProfileData.setTermCodes(List.of(
            de.numcodex.feasibility_gui_backend.common.api.TermCode.builder()
            .code(tc.getCode())
            .display(tc.getDisplay())
            .system(tc.getSystem())
            .version(tc.getVersion())
            .build())
        );
      } else {
        criteriaProfileData.setTermCodes(List.of());
      }
      results.add(criteriaProfileData);
    }

    return results;
  }
}
