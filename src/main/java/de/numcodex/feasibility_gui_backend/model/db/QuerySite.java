package de.numcodex.feasibility_gui_backend.model.db;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Data
@Entity
public class QuerySite {

  @EmbeddedId
  private QuerySiteId id;

  @Column(name = "posted_at", insertable = false)
  private Timestamp postedAt;

  @Data
  @Embeddable
  public static class QuerySiteId implements Serializable {
    @Column(name = "query_id")
    private String queryId;

    @Column(name = "site_id")
    private Long siteId;
  }
}
