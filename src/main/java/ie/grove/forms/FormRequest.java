package ie.grove.forms;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "form_request")
public class FormRequest extends TenantEntity {

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "guardian_id")
    private Long guardianId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormRequestStatus status;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signature_png_url")
    private String signaturePngUrl;

    @Column(name = "signed_pdf_url")
    private String signedPdfUrl;

    @Column(name = "signer_ip")
    private String signerIp;

    protected FormRequest() {
    }

    public FormRequest(Long templateId, Long childId, FormRequestStatus status) {
        this.templateId = templateId;
        this.childId = childId;
        this.status = status;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getChildId() {
        return childId;
    }

    public Long getGuardianId() {
        return guardianId;
    }

    public void setGuardianId(Long guardianId) {
        this.guardianId = guardianId;
    }

    public FormRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FormRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public String getSignaturePngUrl() {
        return signaturePngUrl;
    }

    public void setSignaturePngUrl(String signaturePngUrl) {
        this.signaturePngUrl = signaturePngUrl;
    }

    public String getSignedPdfUrl() {
        return signedPdfUrl;
    }

    public void setSignedPdfUrl(String signedPdfUrl) {
        this.signedPdfUrl = signedPdfUrl;
    }

    public String getSignerIp() {
        return signerIp;
    }

    public void setSignerIp(String signerIp) {
        this.signerIp = signerIp;
    }
}
