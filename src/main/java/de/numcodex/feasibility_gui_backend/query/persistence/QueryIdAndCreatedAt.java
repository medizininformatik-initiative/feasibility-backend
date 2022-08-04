package de.numcodex.feasibility_gui_backend.query.persistence;


import java.sql.Timestamp;

public interface QueryIdAndCreatedAt {
  Long getId();
  Timestamp getCreatedAt();
}
