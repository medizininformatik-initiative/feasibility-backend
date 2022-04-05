package de.numcodex.feasibility_gui_backend.terminology.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UiProfileRepository extends JpaRepository<UiProfileContent, Long> {
    @Query("select UI_Profile from UI_PROFILE_TABLE where UI_Profile.system = :system and UI_Profile.code = :code and UI_Profile.version = :version")
    UiProfileContent findUiProfileByCoding(@Param("system") String system, @Param("code") String code,
        @Param("version") String version);
}
