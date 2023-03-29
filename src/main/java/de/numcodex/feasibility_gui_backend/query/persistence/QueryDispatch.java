package de.numcodex.feasibility_gui_backend.query.persistence;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import java.io.Serializable;
import java.sql.Timestamp;
import lombok.Data;

@Data
@Entity
public class QueryDispatch {

    @EmbeddedId
    private QueryDispatchId id;

    @MapsId("queryId")
    @JoinColumn(referencedColumnName = "id", name = "query_id", nullable = false)
    @ManyToOne(fetch = LAZY)
    private Query query;

    @Column(name = "dispatched_at", insertable = false, updatable = false)
    private Timestamp dispatchedAt;

    @Data
    @Embeddable
    public static class QueryDispatchId implements Serializable {
        @Column(name = "query_id")
        private Long queryId;

        @Column(name = "external_query_id")
        private String externalId;

        @Convert(converter = BrokerTypeConverter.class)
        @Column(name = "broker_type")
        private BrokerClientType brokerType;
    }
}
