package ie.grove.account;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Authenticated principal: global identity plus the creche it resolves to.
 * {@code crecheId} is the first membership's creche (docs/spec.md §5 — multi-creche
 * users get a picker in a later chunk) and feeds {@code TenantContext}.
 */
public final class GroveUserDetails extends User {

    private final Long userId;
    private final Long crecheId;

    public GroveUserDetails(AppUser user, Long crecheId,
            Collection<? extends GrantedAuthority> authorities) {
        // Passwordless users (passkeys/magic link, later chunk) must never match a
        // submitted password: random secret that cannot be typed.
        super(user.getEmail(),
              user.getPasswordHash() == null || user.getPasswordHash().isBlank()
                      ? "{noop}" + java.util.UUID.randomUUID()
                      : user.getPasswordHash(),
              user.getStatus() == UserStatus.ACTIVE, true, true, true, authorities);
        this.userId = user.getId();
        this.crecheId = crecheId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCrecheId() {
        return crecheId;
    }
}
