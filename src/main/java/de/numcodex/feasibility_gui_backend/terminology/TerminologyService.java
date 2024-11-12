package de.numcodex.feasibility_gui_backend.terminology;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.numcodex.feasibility_gui_backend.terminology.api.*;
import de.numcodex.feasibility_gui_backend.terminology.persistence.*;
import de.numcodex.feasibility_gui_backend.terminology.persistence.UiProfile;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Service
@Slf4j
public class TerminologyService {

  private final UiProfileRepository uiProfileRepository;

  private final TermCodeRepository termCodeRepository;


  @Getter
  private final List<TerminologySystemEntry> terminologySystems;

  @NonNull
  private ObjectMapper jsonUtil;


  public TerminologyService(@Value("${app.terminologySystemsFile}") String terminologySystemsFilename,
                            UiProfileRepository uiProfileRepository,
                            TermCodeRepository termCodeRepository,
                            ObjectMapper jsonUtil) throws IOException {
    this.uiProfileRepository = uiProfileRepository;
    this.termCodeRepository = termCodeRepository;
    this.jsonUtil = jsonUtil;
    this.terminologySystems = jsonUtil.readValue(new URL("file:" + terminologySystemsFilename), new TypeReference<>() {});
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

  public boolean isExistingTermCode(String system, String code) {
    return termCodeRepository.existsTermCode(system, code);
  }

  public static int min(int... numbers) {
    return Arrays.stream(numbers)
        .min().orElse(Integer.MAX_VALUE);
  }

  public List<CriteriaProfileData> getCriteriaProfileData(List<String> criteriaIds) {
    List<CriteriaProfileData> results = new ArrayList<>();

    for (String id : criteriaIds) {
      TermCode tc = termCodeRepository.findTermCodeByContextualizedTermcodeHash(id).orElse(null);
      Context c = termCodeRepository.findContextByContextualizedTermcodeHash(id).orElse(null);
      de.numcodex.feasibility_gui_backend.terminology.api.UiProfile uiProfile;
      de.numcodex.feasibility_gui_backend.common.api.TermCode context;
      List<de.numcodex.feasibility_gui_backend.common.api.TermCode> termCodes = new ArrayList<>();
      try {
        uiProfile = jsonUtil.readValue(getUiProfile(id), de.numcodex.feasibility_gui_backend.terminology.api.UiProfile.class);
      } catch (UiProfileNotFoundException | JsonProcessingException e) {
        log.debug("Error trying to read ui profile", e);
        uiProfile = null;
      }
      if (c != null) {
        context = de.numcodex.feasibility_gui_backend.common.api.TermCode.builder()
            .code(c.getCode())
            .display(c.getDisplay())
            .system(c.getSystem())
            .version(c.getVersion())
            .build();
      } else {
        context = null;
      }
      if (tc != null) {
        termCodes.add(
            de.numcodex.feasibility_gui_backend.common.api.TermCode.builder()
                .code(tc.getCode())
                .display(tc.getDisplay())
                .system(tc.getSystem())
                .version(tc.getVersion())
                .build()
        );
      }
      results.add(
          CriteriaProfileData.builder()
              .id(id)
              .uiProfile(uiProfile)
              .context(context)
              .termCodes(termCodes)
              .build()
      );
    }

    return results;
  }
}
