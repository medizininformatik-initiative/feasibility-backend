package de.numcodex.feasibility_gui_backend.query.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DataqueryRepository extends JpaRepository<Dataquery, Long> {
  List<Dataquery> findAllByCreatedBy(String userId);

  @Query(value = "SELECT dq from Dataquery dq WHERE dq.expiresAt IS NULL AND dq.createdBy = ?1")
  List<Dataquery> findAllNonTemporaryByCreatedBy(String userId);

  @Query(value = "SELECT COUNT(*) FROM Dataquery WHERE createdBy = ?1 AND resultSize IS NOT NULL")
  Long countByCreatedByWhereResultIsNotNull(String userId);

  @Modifying
  @Transactional
  @Query(value = "DELETE FROM Dataquery dq WHERE dq.expiresAt < CURRENT_TIMESTAMP")
  int deleteExpired();
}
