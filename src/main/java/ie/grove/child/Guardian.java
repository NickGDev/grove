package ie.grove.child;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Parent link: which user may collect / is billed for a child. */
@Entity
@Table(name = "guardian")
public class Guardian extends TenantEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    private String relationship;

    @Column(name = "can_collect", nullable = false)
    private boolean canCollect;

    @Column(name = "is_billing_contact", nullable = false)
    private boolean isBillingContact;

    protected Guardian() {
    }

    public Guardian(Long userId, Long childId, boolean canCollect, boolean isBillingContact) {
        this.userId = userId;
        this.childId = childId;
        this.canCollect = canCollect;
        this.isBillingContact = isBillingContact;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getChildId() {
        return childId;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public boolean isCanCollect() {
        return canCollect;
    }

    public void setCanCollect(boolean canCollect) {
        this.canCollect = canCollect;
    }

    public boolean isBillingContact() {
        return isBillingContact;
    }

    public void setBillingContact(boolean billingContact) {
        isBillingContact = billingContact;
    }
}
