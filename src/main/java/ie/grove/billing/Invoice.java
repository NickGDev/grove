package ie.grove.billing;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** One invoice per family per period — lines grouped by billing contact's family key. */
@Entity
@Table(name = "invoice")
public class Invoice extends TenantEntity {

    @Column(name = "family_key", nullable = false)
    private String familyKey;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(name = "total_cents", nullable = false)
    private int totalCents;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "pdf_url")
    private String pdfUrl;

    protected Invoice() {
    }

    public Invoice(String familyKey, LocalDate periodStart, LocalDate periodEnd, String number,
            InvoiceStatus status, int totalCents) {
        this.familyKey = familyKey;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.number = number;
        this.status = status;
        this.totalCents = totalCents;
    }

    public String getFamilyKey() {
        return familyKey;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public int getTotalCents() {
        return totalCents;
    }

    public void setTotalCents(int totalCents) {
        this.totalCents = totalCents;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
}
