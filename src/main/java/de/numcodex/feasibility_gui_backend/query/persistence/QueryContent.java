package de.numcodex.feasibility_gui_backend.query.persistence;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

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
