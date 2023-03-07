package de.numcodex.feasibility_gui_backend.query.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.sql.Timestamp;
import lombok.Data;

@Data
@Entity
public class Query {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "id", name = "query_content_id")
    private QueryContent queryContent;

    @OneToOne(mappedBy = "query", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SavedQuery savedQuery;
}
