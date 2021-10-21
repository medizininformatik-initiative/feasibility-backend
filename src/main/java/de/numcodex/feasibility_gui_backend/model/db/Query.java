package de.numcodex.feasibility_gui_backend.model.db;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
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

    @OneToMany(mappedBy = "query", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Result> results;

    @Column(name = "title")
    private String title;

    @Column(name = "comment")
    private String comment;

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
