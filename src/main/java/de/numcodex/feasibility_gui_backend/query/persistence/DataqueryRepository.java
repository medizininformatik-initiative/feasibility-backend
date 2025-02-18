package de.numcodex.feasibility_gui_backend.query.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DataqueryRepository extends JpaRepository<Dataquery, Long> {
  List<Dataquery> findAllByCreatedBy(String userId);

  @Query(value = "SELECT COUNT(*) FROM Dataquery WHERE createdBy = ?1 AND resultSize IS NOT NULL")
  Long countByCreatedByWhereResultIsNotNull(String userId);
}
