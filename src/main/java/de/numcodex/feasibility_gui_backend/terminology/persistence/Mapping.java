package de.numcodex.feasibility_gui_backend.terminology.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode
public class Mapping {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private int id;
    @Basic
    @Column(name = "name", nullable = false, length = -1)
    private String name;
    @Basic
    @Column(name = "type", nullable = false, length = -1)
    private String type;
    @Basic
    @Column(name = "content", columnDefinition = "json", nullable = false)
    private String content;
}
