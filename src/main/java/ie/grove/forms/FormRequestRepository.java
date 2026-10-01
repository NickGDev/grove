package ie.grove.forms;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormRequestRepository extends JpaRepository<FormRequest, Long> {

    List<FormRequest> findByChildId(Long childId);
}
