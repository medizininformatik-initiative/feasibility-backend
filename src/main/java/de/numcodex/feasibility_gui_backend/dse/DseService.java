package de.numcodex.feasibility_gui_backend.dse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfile;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfileTreeNode;
import de.numcodex.feasibility_gui_backend.dse.persistence.DseProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DseService {

  @Value("classpath:de/numcodex/feasibility_gui_backend/dse/profile_tree.json")
  private Resource profileTreeResource;

  private final DseProfileRepository dseProfileRepository;

  private final ObjectMapper objectMapper;

  public DseService(DseProfileRepository dseProfileRepository, ObjectMapper objectMapper) {
    this.dseProfileRepository = dseProfileRepository;
    this.objectMapper = objectMapper;
  }

  public DseProfileTreeNode getProfileTree() {
    try {
      var profileTreeString = profileTreeResource.getContentAsString(StandardCharsets.UTF_8);
      return objectMapper.readValue(profileTreeString, DseProfileTreeNode.class);
    } catch (IOException e) {
      throw new DseProfileException("Could not read profile tree: " + e.getMessage());
    }
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
