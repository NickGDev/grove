package ie.grove.billing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeItemRepository extends JpaRepository<FeeItem, Long> {

    List<FeeItem> findByChildId(Long childId);
}
