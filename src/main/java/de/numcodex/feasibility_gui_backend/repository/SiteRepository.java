package de.numcodex.feasibility_gui_backend.repository;

import de.numcodex.feasibility_gui_backend.model.db.Site;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SiteRepository extends JpaRepository<Site, Long> {
    @Query("SELECT s FROM Site s WHERE s.siteName = ?1")
    Optional<Site> findBySiteName(String siteName);

    @Query("SELECT id from Site")
    List<Integer> getSiteIds();
}
