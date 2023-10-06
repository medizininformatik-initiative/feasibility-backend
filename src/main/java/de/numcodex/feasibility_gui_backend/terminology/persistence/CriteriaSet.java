package de.numcodex.feasibility_gui_backend.terminology.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "criteria_set", schema = "public")
@Data
@EqualsAndHashCode
public class CriteriaSet {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private int id;
    @Basic
    @Column(name = "url", nullable = false, length = -1)
    private String url;
}
