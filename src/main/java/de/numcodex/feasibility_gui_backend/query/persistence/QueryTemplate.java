package de.numcodex.feasibility_gui_backend.query.persistence;

import java.sql.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class QueryTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "id", name = "query_id")
    private Query query;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "comment")
    private String comment;

    @Column(name = "last_modified", insertable = false)
    private Timestamp lastModified;
}
