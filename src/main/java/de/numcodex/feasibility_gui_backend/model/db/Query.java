package de.numcodex.feasibility_gui_backend.model.db;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Type;

@Data
@Entity
public class Query {

    @Id
    private String id;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(referencedColumnName = "id", name = "query_content_id")
    private QueryContent queryContent;

    @Enumerated(EnumType.STRING)
    @Type(type = "status_type")
    @Column(columnDefinition = "status")
    private QueryStatus status;
}
