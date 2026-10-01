package ie.grove.billing;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "fee_item")
public class FeeItem extends TenantEntity {

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(nullable = false)
    private String name;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    protected FeeItem() {
    }

    public FeeItem(Long childId, String name, int amountCents, Frequency frequency) {
        this.childId = childId;
        this.name = name;
        this.amountCents = amountCents;
        this.frequency = frequency;
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

    public int getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(int amountCents) {
        this.amountCents = amountCents;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
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
