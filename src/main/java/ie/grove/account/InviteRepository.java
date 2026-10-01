package ie.grove.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteRepository extends JpaRepository<Invite, Long> {

    Optional<Invite> findByTokenHash(String tokenHash);
}
