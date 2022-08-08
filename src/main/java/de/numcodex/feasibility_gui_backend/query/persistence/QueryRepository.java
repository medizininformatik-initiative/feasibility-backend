package de.numcodex.feasibility_gui_backend.query.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryRepository extends JpaRepository<Query, Long> {

  @org.springframework.data.jpa.repository.Query("SELECT t FROM Query t WHERE t.createdBy = ?1")
  Optional<List<Query>> findByAuthor(String authorId);

  @org.springframework.data.jpa.repository.Query("SELECT t FROM Query t left join SavedQuery s on t.id = s.query.id WHERE t.createdBy = ?1 AND s.id IS NOT NULL")
  Optional<List<Query>> findSavedQueriesByAuthor(String authorId);

  @org.springframework.data.jpa.repository.Query("SELECT t.createdBy FROM Query t WHERE t.id = ?1")
  Optional<String> getAuthor(Long queryId);

  @org.springframework.data.jpa.repository.Query(value = """
    SELECT count (*) FROM query WHERE created_by = ?1 AND created_at > (current_timestamp - (?2 * interval '1 minute'))""", nativeQuery = true)
  Long countQueriesByAuthorInTheLastNMinutes(String authorId, int minutes);
}
