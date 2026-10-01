package ie.grove.messaging;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation")
public class Conversation extends TenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    private String title;

    protected Conversation() {
    }

    public Conversation(ConversationType type, String title) {
        this.type = type;
        this.title = title;
    }

    public ConversationType getType() {
        return type;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
