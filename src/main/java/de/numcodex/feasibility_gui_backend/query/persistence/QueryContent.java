package de.numcodex.feasibility_gui_backend.query.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
public class QueryContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_content", nullable = false)
    private String queryContent;

    @Column(columnDefinition = "TEXT")
    private String hash;

    public QueryContent(String queryContent) {
        this.queryContent = queryContent;
    }
}
