package ie.grove.account;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findByUserId(Long userId);

    List<Membership> findByUserIdOrderById(Long userId);
}
