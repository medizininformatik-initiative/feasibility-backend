package de.numcodex.feasibility_gui_backend.query.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {
    @Query("SELECT r FROM Result r WHERE r.id.queryId = ?1 AND r.resultType = ?2")
    List<Result> findByQueryAndStatus(Long queryId, ResultType resultType);
}
