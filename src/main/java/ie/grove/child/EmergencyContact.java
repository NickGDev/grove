package ie.grove.child;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "emergency_contact")
public class EmergencyContact extends TenantEntity {

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String relationship;

    protected EmergencyContact() {
    }

    public EmergencyContact(Long childId, String name) {
        this.childId = childId;
        this.name = name;
    }

    public Long getChildId() {
        return childId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}
