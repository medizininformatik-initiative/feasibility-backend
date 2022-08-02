package de.numcodex.feasibility_gui_backend.query.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QueryTemplateRepository extends JpaRepository<QueryTemplate, Long> {

  @Query("SELECT qt FROM QueryTemplate qt left join Query q ON qt.query.id = q.id WHERE q.createdBy = ?1")
  List<QueryTemplate> findByAuthor(String authorId);
}
