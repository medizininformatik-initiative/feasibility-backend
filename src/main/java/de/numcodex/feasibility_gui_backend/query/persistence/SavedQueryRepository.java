package de.numcodex.feasibility_gui_backend.query.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SavedQueryRepository extends JpaRepository<SavedQuery, Long> {

  @Query("SELECT sq FROM SavedQuery sq left join Query q ON sq.query.id = q.id WHERE q.id = ?1")
  Optional<SavedQuery> findByQueryId(Long queryId);
}
