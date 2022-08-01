package de.numcodex.feasibility_gui_backend.terminology.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UiProfileRepository extends JpaRepository<UiProfileContent, Long> {
    @Query("from UI_PROFILE u where u.system = :system and u.code = :code and u.version = :version")
    List<UiProfileContent> findUiProfileByCoding(@Param("system") String system, @Param("code") String code,
        @Param("version") String version);

    @Query("from UI_PROFILE u where u.system = :system and u.code = :code")
    List<UiProfileContent> findUiProfileByCoding(@Param("system") String system, @Param("code") String code);
}
