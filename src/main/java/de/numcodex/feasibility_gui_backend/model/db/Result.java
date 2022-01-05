package de.numcodex.feasibility_gui_backend.model.db;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import org.hibernate.annotations.Type;

@Data
@Entity
public class Result {

    @EmbeddedId
    private ResultId id;

    @MapsId("queryId")
    @JoinColumn(referencedColumnName = "id", name = "query_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Query query;

    @MapsId("siteId")
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

    @Data
    @Embeddable
    public static class ResultId implements Serializable {
        @Column(name = "query_id")
        private Long queryId;

        @Column(name = "site_id")
        private Long siteId;
    }
}
