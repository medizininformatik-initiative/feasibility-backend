package de.numcodex.feasibility_gui_backend.query.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.sql.Timestamp;
import lombok.Data;

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

    @Column(columnDefinition = "result_type")
    @Convert(converter = ResultTypeConverter.class)
    private ResultType resultType;

    private Integer result;

    @Column(insertable = false)
    private Timestamp receivedAt;
}
