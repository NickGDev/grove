package ie.grove.forms;

import ie.grove.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "form_template")
public class FormTemplate extends TenantEntity {

    @Column(nullable = false)
    private String title;

    @Column(name = "body_html")
    private String bodyHtml;

    @Column(name = "requires_signature", nullable = false)
    private boolean requiresSignature;

    protected FormTemplate() {
    }

    public FormTemplate(String title, boolean requiresSignature) {
        this.title = title;
        this.requiresSignature = requiresSignature;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public boolean isRequiresSignature() {
        return requiresSignature;
    }

    public void setRequiresSignature(boolean requiresSignature) {
        this.requiresSignature = requiresSignature;
    }
}
