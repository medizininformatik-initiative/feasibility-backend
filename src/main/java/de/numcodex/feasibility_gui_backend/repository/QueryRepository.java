package de.numcodex.feasibility_gui_backend.repository;

import de.numcodex.feasibility_gui_backend.model.db.Query;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryRepository extends JpaRepository<Query, Long> {

  @org.springframework.data.jpa.repository.Query("SELECT t FROM Query t WHERE t.mockId = ?1")
  Optional<Query> findByMockId(String directId);

  @org.springframework.data.jpa.repository.Query("SELECT t FROM Query t WHERE t.directId = ?1")
  Optional<Query> findByDirectId(String directId);

  @org.springframework.data.jpa.repository.Query("SELECT t FROM Query t WHERE t.aktinId = ?1")
  Optional<Query> findByAktinId(String directId);

  @org.springframework.data.jpa.repository.Query("SELECT t FROM Query t WHERE t.dsfId = ?1")
  Optional<Query> findByDsfId(String directId);

}
