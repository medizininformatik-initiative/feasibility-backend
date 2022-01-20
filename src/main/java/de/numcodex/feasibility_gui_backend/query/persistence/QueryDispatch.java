package de.numcodex.feasibility_gui_backend.query.persistence;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

import static javax.persistence.FetchType.LAZY;

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

        @Enumerated(EnumType.STRING)
        @Type(type = "broker_type")
        @Column(name = "broker_type")
        private BrokerClientType brokerType;
    }
}
