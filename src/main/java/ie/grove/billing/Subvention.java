package ie.grove.billing;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "subvention")
public class Subvention extends TenantEntity {

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubventionType type;

    @Column(name = "hours_per_week", nullable = false)
    private double hoursPerWeek;

    @Column(name = "hourly_rate_cents", nullable = false)
    private int hourlyRateCents;

    @Column(name = "reference_code")
    private String referenceCode;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    protected Subvention() {
    }

    public Subvention(Long childId, SubventionType type, double hoursPerWeek, int hourlyRateCents) {
        this.childId = childId;
        this.type = type;
        this.hoursPerWeek = hoursPerWeek;
        this.hourlyRateCents = hourlyRateCents;
    }

    public Long getChildId() {
        return childId;
    }

    public SubventionType getType() {
        return type;
    }

    public void setType(SubventionType type) {
        this.type = type;
    }

    public double getHoursPerWeek() {
        return hoursPerWeek;
    }

    public void setHoursPerWeek(double hoursPerWeek) {
        this.hoursPerWeek = hoursPerWeek;
    }

    public int getHourlyRateCents() {
        return hourlyRateCents;
    }

    public void setHourlyRateCents(int hourlyRateCents) {
        this.hourlyRateCents = hourlyRateCents;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }
}
