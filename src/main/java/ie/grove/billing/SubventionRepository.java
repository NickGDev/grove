package ie.grove.billing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubventionRepository extends JpaRepository<Subvention, Long> {

    List<Subvention> findByChildId(Long childId);
}
