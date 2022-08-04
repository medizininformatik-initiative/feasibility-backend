package de.numcodex.feasibility_gui_backend.query.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryRepository extends JpaRepository<Query, Long> {

  @org.springframework.data.jpa.repository.Query("SELECT t FROM Query t WHERE t.createdBy = ?1")
  Optional<List<QueryIdAndCreatedAt>> findByAuthor(String authorId);

  @org.springframework.data.jpa.repository.Query("SELECT t.createdBy FROM Query t WHERE t.id = ?1")
  Optional<String> getAuthor(Long queryId);
}
