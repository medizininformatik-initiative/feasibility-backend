package de.numcodex.feasibility_gui_backend.query.persistence;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

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
