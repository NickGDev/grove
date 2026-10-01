package ie.grove.attendance;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "diary_entry")
public class DiaryEntry extends TenantEntity {

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaryEntryType type;

    private String body;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "visible_to_parents", nullable = false)
    private boolean visibleToParents;

    protected DiaryEntry() {
    }

    public DiaryEntry(Long childId, DiaryEntryType type, LocalDateTime recordedAt) {
        this.childId = childId;
        this.type = type;
        this.recordedAt = recordedAt;
    }

    public Long getChildId() {
        return childId;
    }

    public DiaryEntryType getType() {
        return type;
    }

    public void setType(DiaryEntryType type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(Long recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public boolean isVisibleToParents() {
        return visibleToParents;
    }

    public void setVisibleToParents(boolean visibleToParents) {
        this.visibleToParents = visibleToParents;
    }
}
