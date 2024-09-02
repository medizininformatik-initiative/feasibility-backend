package de.numcodex.feasibility_gui_backend.dse.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DseProfileRepository extends JpaRepository<DseProfile, Long> {

  Optional<DseProfile> findByUrl(String url);
}
