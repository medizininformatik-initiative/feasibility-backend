package de.numcodex.feasibility_gui_backend.query.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QueryTemplateRepository extends JpaRepository<QueryTemplate, Long> {

  @Query("SELECT qt FROM QueryTemplate qt left join Query q ON qt.query.id = q.id WHERE q.createdBy = ?1")
  List<QueryTemplate> findByAuthor(String authorId);

  @Query("select case when count(qt) > 0 then true else false end from QueryTemplate qt left join Query q on qt.query.id = q.id where qt.label =?1 and q.createdBy = ?2")
  boolean existsQueryTemplateByLabelAndUserId(String label, String authorId);
}
