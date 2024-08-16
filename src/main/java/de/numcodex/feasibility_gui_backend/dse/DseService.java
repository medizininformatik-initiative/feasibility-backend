package de.numcodex.feasibility_gui_backend.dse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfile;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfileTreeNode;
import de.numcodex.feasibility_gui_backend.dse.persistence.DseProfileRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DseService {

  @Getter
  private final DseProfileTreeNode profileTree;

  private final DseProfileRepository dseProfileRepository;

  private final ObjectMapper objectMapper;

  public DseService(@Value("${app.dseProfileTreeFile}") String dseProfileTreeFilename,
                    DseProfileRepository dseProfileRepository,
                    ObjectMapper objectMapper) throws IOException {
    this.dseProfileRepository = dseProfileRepository;
    this.objectMapper = objectMapper;
    this.profileTree = readProfileTree(dseProfileTreeFilename);
  }

  public DseProfileTreeNode readProfileTree(String dseProfileTreeFilename) throws IOException {
    return objectMapper.readValue(
        new URL("file:" + dseProfileTreeFilename), DseProfileTreeNode.class);
  }

  public List<DseProfile> getProfileData(List<String> profileIds) {
    var results = new ArrayList<DseProfile>();

    for (String profileId : profileIds) {
      var dseProfile = dseProfileRepository.findByUrl(profileId);
      if (dseProfile.isPresent()) {
        try {
          results.add(objectMapper.readValue(dseProfile.get().getEntry(), DseProfile.class));
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      } else {
        results.add(DseProfile.builder()
            .url(profileId)
            .errorCode("TBD-00000")
            .errorCause("profile not found")
            .build());
      }
    }

    return results;
  }
}
