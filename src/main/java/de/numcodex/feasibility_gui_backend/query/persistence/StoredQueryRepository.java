package de.numcodex.feasibility_gui_backend.query.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StoredQueryRepository extends JpaRepository<StoredQuery, Long> {

  @Query("SELECT sq FROM StoredQuery sq WHERE sq.createdBy = ?1")
  List<StoredQuery> findByAuthor(String authorId);
}
