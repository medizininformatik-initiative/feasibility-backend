package de.numcodex.feasibility_gui_backend.terminology.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MappingRepository extends JpaRepository<Mapping, Long> {

    @Query("select m from ContextualizedTermCode ct left join Mapping m on ct.uiProfileId = m.id where ct.contextTermcodeHash = :contextualizedTermcodeHash")
    Optional<Mapping> findByContextualizedTermcodeHash(@Param("contextualizedTermcodeHash") String contextualizedTermcodeHash);
}
