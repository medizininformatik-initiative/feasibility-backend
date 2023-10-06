package de.numcodex.feasibility_gui_backend.terminology.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "contextualized_termcode", schema = "public")
@Data
@EqualsAndHashCode
public class ContextualizedTermCode {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "context_termcode_hash", nullable = false, length = -1)
    private String contextTermcodeHash;
    @Basic
    @Column(name = "context_id", nullable = false)
    private int contextId;
    @Basic
    @Column(name = "termcode_id", nullable = false)
    private int termCodeId;
    @Basic
    @Column(name = "mapping_id", nullable = true)
    private Integer mappingId;
    @Basic
    @Column(name = "ui_profile_id", nullable = true)
    private Integer uiProfileId;
}
