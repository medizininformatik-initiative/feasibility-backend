package de.numcodex.feasibility_gui_backend.repository;

import de.numcodex.feasibility_gui_backend.model.db.Result;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ResultRepository extends JpaRepository<Result, Long> {
    @Query("SELECT t FROM Result t WHERE t.queryId = ?1")
    List<Result> findByQueryId(String queryId);

    @Query("SELECT t FROM Result t WHERE t.siteId = ?1")
    List<Result> findBySiteId(String siteId);
}
