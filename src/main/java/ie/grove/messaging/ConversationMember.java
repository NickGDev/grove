package ie.grove.messaging;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_member")
public class ConversationMember extends TenantEntity {

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    protected ConversationMember() {
    }

    public ConversationMember(Long conversationId, Long userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
}
