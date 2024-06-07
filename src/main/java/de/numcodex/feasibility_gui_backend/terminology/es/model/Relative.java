package de.numcodex.feasibility_gui_backend.terminology.es.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;

@Data
@Builder
public class Relative {

  private String name;
  @Field(name = "contextualized_termcode_hash")
  private String contextualizedTermcodeHash;
}
