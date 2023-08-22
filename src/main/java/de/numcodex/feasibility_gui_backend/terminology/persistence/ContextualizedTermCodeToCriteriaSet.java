package de.numcodex.feasibility_gui_backend.terminology.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "contextualized_termcode_to_criteria_set", schema = "public")
@Data
@EqualsAndHashCode
public class ContextualizedTermCodeToCriteriaSet {
    @Id
    @Basic
    @Column(name = "context_termcode_hash", nullable = false, length = -1)
    private String contextTermcodeHash;
    @Id
    @Basic
    @Column(name = "criteria_set_id", nullable = false)
    private int criteriaSetId;
}
