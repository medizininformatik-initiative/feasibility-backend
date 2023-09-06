package de.numcodex.feasibility_gui_backend.query.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SavedQueryRepository extends JpaRepository<SavedQuery, Long> {

  @Query("SELECT sq FROM SavedQuery sq left join Query q ON sq.query.id = q.id WHERE q.id = ?1")
  Optional<SavedQuery> findByQueryId(Long queryId);

  @Query("select case when count(sq) > 0 then true else false end from SavedQuery sq left join Query q on sq.query.id = q.id where sq.label =?1 and q.createdBy = ?2")
  boolean existsSavedQueryByLabelAndUserId(String label, String authorId);
}
