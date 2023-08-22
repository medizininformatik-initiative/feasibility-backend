package de.numcodex.feasibility_gui_backend.terminology.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TermCodeRepository extends JpaRepository<TermCode, Long> {

    @Query("select case when count(t) > 0 then true else false end from TermCode t where t.code = :code and t.system = :system and t.version = :version")
    boolean existsTermCode(@Param("system") String system, @Param("code") String code, @Param("version") String version);

    @Query("select case when count(t) > 0 then true else false end from TermCode t where t.code = :code and t.system = :system")
    boolean existsTermCode(@Param("system") String system, @Param("code") String code);

}
