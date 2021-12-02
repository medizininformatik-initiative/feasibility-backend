package de.numcodex.feasibility_gui_backend.repository;

import de.numcodex.feasibility_gui_backend.model.db.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {
    @Query("SELECT t FROM Result t WHERE t.id.queryId = ?1")
    List<Result> findByQueryId(String queryId);
}
