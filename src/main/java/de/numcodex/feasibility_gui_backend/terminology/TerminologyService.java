package de.numcodex.feasibility_gui_backend.terminology;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.terminology.api.CategoryEntry;
import de.numcodex.feasibility_gui_backend.terminology.api.TerminologyEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TerminologyService {

  private  String uiProfilePath;
  private static final List<String> SORTED_CATEGORIES = List.of("Einwilligung", "Biobank", "Diagnose", "Fall", "Laborbefund", "Medikation", "Person", "Prozedur", "GECCO");
  private Map<UUID, TerminologyEntry> terminologyEntries = new HashMap<>();
  private List<CategoryEntry> categoryEntries = new ArrayList<>();
  private Map<UUID, TerminologyEntry> terminologyEntriesWithOnlyDirectChildren = new HashMap<>();
  private Map<UUID, Set<TerminologyEntry>> selectableEntriesByCategory = new HashMap<>();

  public TerminologyService(@Value("${app.ontologyFolder}") String uiProfilePath) {
    this.uiProfilePath = uiProfilePath;
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
            new URL("file:" + uiProfilePath + "/" + filename),
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
    System.out.println("####################");
    System.out.println(uiProfilePath);

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

  //TODO: Unknown key!
  public TerminologyEntry getEntry(UUID nodeId) {
    return terminologyEntriesWithOnlyDirectChildren.get(nodeId);
  }

  public List<CategoryEntry> getCategories() {
    categoryEntries.sort(Comparator.comparing(value -> SORTED_CATEGORIES.indexOf(value.getDisplay())));
    return categoryEntries;
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
      System.out.println(allSelectableEntries.size());
      return allSelectableEntries.stream()
          .filter((terminologyEntry -> matchesQuery(query, terminologyEntry))).sorted((Comparator
              .comparingInt(val -> val.getDisplay().length()))).limit(20)
          .collect(Collectors.toList());
    }
  }

  public List<TerminologyEntry> getSelectableEntries2(String query, UUID categoryId) {
    if (categoryId != null) {
      return selectableEntriesByCategory.get(categoryId).stream()
          .sorted(Comparator
              .comparingDouble((TerminologyEntry val) -> diceCoefficientOptimized(val.getDisplay(), query)).reversed()).limit(20)
          .collect(Collectors.toList());
    } else {
      Set<TerminologyEntry> allSelectableEntries = new HashSet<>();
      for (var selectableEntries : selectableEntriesByCategory.values()) {
        allSelectableEntries.addAll(selectableEntries);
      }
      System.out.println(allSelectableEntries.size());
      return allSelectableEntries.stream()
          .sorted(Comparator
              .comparingDouble((TerminologyEntry val) -> diceCoefficientOptimized(val.getDisplay(), query)).reversed()).limit(20)
          .collect(Collectors.toList());
    }
  }

  public static double diceCoefficientOptimized(String s, String t)
  {
    s = s.toLowerCase();
    t = t.toLowerCase();
    // Verifying the input:
    // Quick check to catch identical objects:
    if (Objects.equals(s, t)) {
      return 1;
    }
    // avoid exception for single character searches
    if (s.length() < 2 || t.length() < 2) {
      return 0;
    }

    double result = 0;
    var subStrings = s.split(" ");
    if(subStrings.length > 1) {
      for (var subString : s.split(" ")) {
        var coefficient = diceCoefficientOptimized(subString, t);
        if (coefficient > result) {
          result = coefficient;
        }
      }
    }


    // Create the bigrams for string s:
    final int n = s.length()-1;
    final int[] sPairs = new int[n];
    for (int i = 0; i <= n; i++)
      if (i == 0)
        sPairs[i] = s.charAt(i) << 16;
      else if (i == n)
        sPairs[i-1] |= s.charAt(i);
      else
        sPairs[i] = (sPairs[i-1] |= s.charAt(i)) << 16;

    // Create the bigrams for string t:
    final int m = t.length()-1;
    final int[] tPairs = new int[m];
    for (int i = 0; i <= m; i++)
      if (i == 0)
        tPairs[i] = t.charAt(i) << 16;
      else if (i == m)
        tPairs[i-1] |= t.charAt(i);
      else
        tPairs[i] = (tPairs[i-1] |= t.charAt(i)) << 16;

    // Sort the bigram lists:
    Arrays.sort(sPairs);
    Arrays.sort(tPairs);

    // Count the matches:
    int matches = 0, i = 0, j = 0;
    while (i < n && j < m)
    {
      if (sPairs[i] == tPairs[j])
      {
        matches += 2;
        i++;
        j++;
      }
      else if (sPairs[i] < tPairs[j])
        i++;
      else
        j++;
    }

    return Math.max(result, (double) matches / (n + m));
  }

  // From https://www.baeldung.com/java-levenshtein-distance
  static int calculate_levenshtein_distance(String x, String y) {
    x = x.toLowerCase();
    y = y.toLowerCase();
    int[][] dp = new int[x.length() + 1][y.length() + 1];

    for (int i = 0; i <= x.length(); i++) {
      for (int j = 0; j <= y.length(); j++) {
        if (i == 0) {
          dp[i][j] = j;
        }
        else if (j == 0) {
          dp[i][j] = i;
        }
        else {
          dp[i][j] = min(dp[i - 1][j - 1]
                  + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
              dp[i - 1][j] + 1,
              dp[i][j - 1] + 1);
        }
      }
    }

    return dp[x.length()][y.length()];
  }



  public static int costOfSubstitution(char a, char b) {
    return a == b ? 0 : 1;
  }

  public static int min(int... numbers) {
    return Arrays.stream(numbers)
        .min().orElse(Integer.MAX_VALUE);
  }


  private boolean matchesQuery(String query, TerminologyEntry terminologyEntry) {
    return terminologyEntry.getDisplay().toLowerCase().startsWith(query.toLowerCase()) ||
        Arrays.stream(terminologyEntry.getDisplay().toLowerCase().split(" "))
        .anyMatch(var-> var.startsWith(query.toLowerCase())) ||
        (terminologyEntry.getTermCode() != null && terminologyEntry.getTermCode().getCode()
            .startsWith(query));
  }
}
