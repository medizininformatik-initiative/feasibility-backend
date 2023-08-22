package de.numcodex.feasibility_gui_backend.terminology.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "ui_profile", schema = "public")
@Data
@EqualsAndHashCode
public class UiProfile {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private int id;
    @Basic
    @Column(name = "name", nullable = false, length = -1)
    private String name;
    @Basic
    @Column(name = "ui_profile", columnDefinition = "json", nullable = false)
    private String uiProfile;
}
