package ie.grove.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Audit trail for child/invoice/form views and changes. Written, never edited. */
@Entity
@Table(name = "audit_log")
public class AuditLog extends TenantEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String entity;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false)
    private LocalDateTime at;

    protected AuditLog() {
    }

    public AuditLog(Long userId, String action, String entity, Long entityId, LocalDateTime at) {
        this.userId = userId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.at = at;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getEntity() {
        return entity;
    }

    public Long getEntityId() {
        return entityId;
    }

    public LocalDateTime getAt() {
        return at;
    }
}
