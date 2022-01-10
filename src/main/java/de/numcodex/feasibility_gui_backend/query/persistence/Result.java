package de.numcodex.feasibility_gui_backend.query.persistence;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_id", insertable = false, updatable = false, nullable = false)
    private Long queryId;

    @Column(name = "site_id", insertable = false, updatable = false, nullable = false)
    private Long siteId;

    @JoinColumn(referencedColumnName = "id", name = "query_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Query query;

    @JoinColumn(referencedColumnName = "id", name = "site_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Site site;

    @Enumerated(EnumType.STRING)
    @Type(type = "result_type")
    @Column(columnDefinition = "result_type")
    private ResultType resultType;

    private Integer result;

    @Column(insertable = false)
    private Timestamp receivedAt;
}
