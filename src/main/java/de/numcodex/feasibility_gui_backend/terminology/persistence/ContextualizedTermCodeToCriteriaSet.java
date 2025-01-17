package de.numcodex.feasibility_gui_backend.terminology.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Entity
@Table(name = "contextualized_termcode_to_criteria_set", schema = "public")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ContextualizedTermCodeToCriteriaSet {
    @Id
    @Basic
    @Column(name = "context_termcode_hash", nullable = false, length = -1)
    private String contextTermcodeHash;
    @Id
    @Basic
    @Column(name = "criteria_set_id", nullable = false)
    private int criteriaSetId;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ContextualizedTermCodeToCriteriaSet that = (ContextualizedTermCodeToCriteriaSet) o;
        return getContextTermcodeHash() != null && Objects.equals(getContextTermcodeHash(), that.getContextTermcodeHash());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
