package de.numcodex.feasibility_gui_backend.repository;

import de.numcodex.feasibility_gui_backend.model.db.QueryDispatch;
import de.numcodex.feasibility_gui_backend.model.db.QueryDispatch.QueryDispatchId;
import de.numcodex.feasibility_gui_backend.model.db.BrokerClientType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface QueryDispatchRepository extends JpaRepository<QueryDispatch, QueryDispatchId> {
    @Query("SELECT qd FROM QueryDispatch qd WHERE qd.id.externalId = ?1 AND qd.id.brokerType = ?2")
    Optional<QueryDispatch> findByExternalQueryIdAndBrokerType(String externalQueryId, BrokerClientType brokerType);
}
