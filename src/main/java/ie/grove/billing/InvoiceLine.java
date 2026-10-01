package ie.grove.billing;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Fee lines carry positive cents; subvention lines carry negative cents. */
@Entity
@Table(name = "invoice_line")
public class InvoiceLine extends TenantEntity {

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    private String description;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    protected InvoiceLine() {
    }

    public InvoiceLine(Long invoiceId, Long childId, String description, int amountCents) {
        this.invoiceId = invoiceId;
        this.childId = childId;
        this.description = description;
        this.amountCents = amountCents;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public Long getChildId() {
        return childId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(int amountCents) {
        this.amountCents = amountCents;
    }
}
