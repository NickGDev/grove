package ie.grove.billing;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "payment")
public class Payment extends TenantEntity {

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    private String method;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    private String reference;

    protected Payment() {
    }

    public Payment(Long invoiceId, int amountCents, LocalDate paidOn) {
        this.invoiceId = invoiceId;
        this.amountCents = amountCents;
        this.paidOn = paidOn;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(int amountCents) {
        this.amountCents = amountCents;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public void setPaidOn(LocalDate paidOn) {
        this.paidOn = paidOn;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
