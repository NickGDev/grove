package ie.grove.messaging;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "message")
public class Message extends TenantEntity {

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id")
    private Long senderId;

    private String body;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    protected Message() {
    }

    public Message(Long conversationId, Long senderId, String body, LocalDateTime sentAt) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.body = body;
        this.sentAt = sentAt;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
