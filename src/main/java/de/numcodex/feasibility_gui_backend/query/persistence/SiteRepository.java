package de.numcodex.feasibility_gui_backend.query.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {
    @Query("SELECT s FROM Site s WHERE s.siteName = ?1")
    Optional<Site> findBySiteName(String siteName);

    @Query("SELECT id from Site")
    List<Integer> getSiteIds();
}
