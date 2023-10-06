package de.numcodex.feasibility_gui_backend.terminology.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContextualizedTermCodeRepository extends JpaRepository<ContextualizedTermCode, Long> {

        @Query("select ct.contextTermcodeHash from CriteriaSet cs inner join ContextualizedTermCodeToCriteriaSet ctctcs on ctctcs.criteriaSetId = cs.id inner join ContextualizedTermCode ct on ctctcs.contextTermcodeHash = ct.contextTermcodeHash where ct.contextTermcodeHash in :contextTermCodeHashList and cs.url = :criteriaSetUrl")
        List<String> filterByCriteriaSetUrl(@Param("criteriaSetUrl") String criteriaSetUrl,
                 @Param("contextTermCodeHashList") List<String> contextTermCodeHashList);
}
