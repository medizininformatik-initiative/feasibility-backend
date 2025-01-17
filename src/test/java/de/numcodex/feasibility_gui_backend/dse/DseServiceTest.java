package de.numcodex.feasibility_gui_backend.dse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import de.numcodex.feasibility_gui_backend.common.api.DisplayEntry;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfileTreeNode;
import de.numcodex.feasibility_gui_backend.dse.api.LocalizedValue;
import de.numcodex.feasibility_gui_backend.dse.persistence.DseProfile;
import de.numcodex.feasibility_gui_backend.dse.persistence.DseProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class DseServiceTest {

  @Mock
  private DseProfileRepository dseProfileRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private DseService dseService;

  private DseService createDseService() throws IOException {
    return new DseService("src/test/resources/ontology/dse/profile_tree.json", dseProfileRepository, objectMapper);
  }

  @BeforeEach
  public void setup() throws IOException {
    Mockito.reset(dseProfileRepository);
    dseService = createDseService();
  }

  @Test
  void testCreateProfileTreeInstance_throwsOnProfileTreeNotFound() {
    assertThrows(FileNotFoundException.class, ()
        -> new DseService("src/test/this/is/not/found.json", dseProfileRepository, objectMapper));
  }

  @Test
  void testGetProfileTree_succeeds() {

    var profileTree = assertDoesNotThrow(() -> dseService.getProfileTree());

    assertNotNull(profileTree);
    assertInstanceOf(DseProfileTreeNode.class, profileTree);
    assertEquals(profileTree.name(), "Root");
    assertEquals(profileTree.module(), "no-module");
    assertEquals(profileTree.url(), "no-url");
    assertEquals(profileTree.children().size(), 1);
    assertEquals(profileTree.children().get(0).id(), "de5c2903-00aa-4d64-92e5-2161d56e3daf");
  }

  @Test
  void testGetProfileTree_throwsOnObjectMapperError() {
    assertThrows(UnrecognizedPropertyException.class, ()
        -> new DseService("src/test/resources/ontology/dse/bogus_profile_tree.json", dseProfileRepository, objectMapper));
  }

  @Test
  void testGetProfileData_succeedsWithEmptyList() {
    var results = assertDoesNotThrow(() -> dseService.getProfileData(List.of()));

    assertNotNull(results);
    assertEquals(0, results.size());
  }

  @Test
  void testGetProfileData_succeedsWithoutErrors() throws JsonProcessingException {
    doReturn(Optional.of(createDummyDseProfile())).when(dseProfileRepository).findByUrl(any(String.class));

    var results = assertDoesNotThrow(() -> dseService.getProfileData(List.of("1")));

    assertNotNull(results);
    assertEquals(1, results.size());
  }

  @Test
  void testGetProfileData_succeedsWithErrors() throws JsonProcessingException {
    doReturn(Optional.of(createDummyDseProfile())).when(dseProfileRepository).findByUrl("found");
    doReturn(Optional.empty()).when(dseProfileRepository).findByUrl("not-found");

    var results = assertDoesNotThrow(() -> dseService.getProfileData(List.of("found", "not-found")));

    assertNotNull(results);
    assertEquals(2, results.size());
    assertNull(results.get(0).errorCode());
    assertNotNull(results.get(1).errorCode());
  }

  @Test
  void testGetProfileData_throwsOnJsonProcessingException() {
    doReturn(Optional.of(createDummyDseProfileWithBogusEntry())).when(dseProfileRepository).findByUrl(any(String.class));
    assertThrows(RuntimeException.class, () -> dseService.getProfileData(List.of("1")));
  }

  private DseProfile createDummyDseProfile() throws JsonProcessingException {
    var dseProfile = new DseProfile();

    dseProfile.setId(1L);
    dseProfile.setUrl("http://example.com");
    dseProfile.setEntry(objectMapper.writeValueAsString(createDummyDseProfileEntry()));

    return dseProfile;
  }

  private DseProfile createDummyDseProfileWithBogusEntry() {
    var dseProfile = new DseProfile();

    dseProfile.setId(1L);
    dseProfile.setUrl("http://example.com");
    dseProfile.setEntry("something that can't be parsed as dse profile");

    return dseProfile;
  }

  private de.numcodex.feasibility_gui_backend.dse.api.DseProfile createDummyDseProfileEntry() {

    return de.numcodex.feasibility_gui_backend.dse.api.DseProfile.builder()
        .url("http://example.com")
        .display(createDummyDisplayEntry())
        .fields(List.of())
        .filters(List.of())
        .build();
  }

  private DisplayEntry createDummyDisplayEntry() {
    return DisplayEntry.builder()
        .original("some-display")
        .translations(List.of(createDummyTranslation()))
        .build();
  }

  private LocalizedValue createDummyTranslation() {
    return LocalizedValue.builder()
        .language("en")
        .value("display value")
        .build();
  }
}