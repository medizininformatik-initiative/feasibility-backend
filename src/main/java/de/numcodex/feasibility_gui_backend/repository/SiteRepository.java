package de.numcodex.feasibility_gui_backend.repository;

import de.numcodex.feasibility_gui_backend.model.db.Site;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SiteRepository extends JpaRepository<Site, Long> {
    @Query("SELECT t FROM Site t WHERE t.id = ?1")
    Optional<Site> findBySiteId(Long siteId);
    
    @Query("SELECT t FROM Site t WHERE t.aktinIdentifier = ?1")
    Optional<Site> findByAktinIdentifier(String aktinIdentifier);

    @Query("SELECT t FROM Site t WHERE t.dsfIdentifier = ?1")
    Optional<Site> findByDsfIdentifier(String dsfIdentifier);

    @Query("SELECT id from Site")
    List<Integer> getSiteIds();
}
