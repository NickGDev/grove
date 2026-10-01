package ie.grove.billing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceId(Long invoiceId);
}
