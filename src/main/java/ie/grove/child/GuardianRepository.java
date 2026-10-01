package ie.grove.child;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    List<Guardian> findByChildId(Long childId);

    List<Guardian> findByUserId(Long userId);
}
