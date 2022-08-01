package de.numcodex.feasibility_gui_backend.query.persistence;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;

@Data
@Entity
public class StoredQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "structured_query", nullable = false)
    private String queryContent;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "comment")
    private String comment;

    @Column(name = "last_modified", insertable = false)
    private Timestamp lastModified;

    @Column(name = "created_by")
    private String createdBy;
}
