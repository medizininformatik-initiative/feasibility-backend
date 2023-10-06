package de.numcodex.feasibility_gui_backend.terminology.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "termcode", schema = "public")
@Data
@EqualsAndHashCode
public class TermCode {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private int id;
    @Basic
    @Column(name = "system", nullable = false, length = -1)
    private String system;
    @Basic
    @Column(name = "code", nullable = false, length = -1)
    private String code;
    @Basic
    @Column(name = "version", nullable = true, length = -1)
    private String version;
    @Basic
    @Column(name = "display", nullable = false, length = -1)
    private String display;
}
