package de.numcodex.feasibility_gui_backend.dse.v3;

import de.numcodex.feasibility_gui_backend.dse.DseService;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfile;
import de.numcodex.feasibility_gui_backend.dse.api.DseProfileTreeNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("api/v3/dse")
@RestController
@CrossOrigin
public class DseRestController {

  private final DseService dseService;

  public DseRestController(DseService dseService) {
    this.dseService = dseService;
  }

  @GetMapping(value = "profile-tree", produces = MediaType.APPLICATION_JSON_VALUE)
  public DseProfileTreeNode getProfileTree() {
    return dseService.getProfileTree();
  }

  @GetMapping(value = "profile-data", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<DseProfile> getProfileData(@RequestParam List<String> ids) {
    return dseService.getProfileData(ids);
  }
}
