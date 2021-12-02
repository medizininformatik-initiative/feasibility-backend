package de.numcodex.feasibility_gui_backend.repository;

import de.numcodex.feasibility_gui_backend.model.db.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SiteRepository extends JpaRepository<Site, Long> {
    @Query("SELECT t FROM Site t WHERE t.id = ?1")
    Site findBySiteId(String siteId);
}
