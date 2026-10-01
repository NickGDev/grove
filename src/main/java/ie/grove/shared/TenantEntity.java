package ie.grove.shared;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.TenantId;

/**
 * Base for every business entity: identity plus the creche tenant discriminator.
 * {@code crecheId} is never set by hand — Hibernate fills it from
 * {@link TenantContext} on insert and filters every read by it.
 */
@MappedSuperclass
public abstract class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "creche_id", nullable = false)
    private Long crecheId;

    public Long getId() {
        return id;
    }

    public Long getCrecheId() {
        return crecheId;
    }
}
