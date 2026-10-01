package ie.grove.account;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads users + memberships into authorities (ROLE_OWNER/MANAGER/STAFF/PARENT). */
@Service
public class GroveUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;
    private final MembershipRepository memberships;

    public GroveUserDetailsService(AppUserRepository users, MembershipRepository memberships) {
        this.users = users;
        this.memberships = memberships;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username == null ? "" : username.trim().toLowerCase();
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
        // Deactivated users lose access; their records stay (spec.md §4.7).
        if (user.getStatus() == UserStatus.DEACTIVATED) {
            throw new org.springframework.security.authentication.DisabledException(
                    "Account is deactivated");
        }
        List<Membership> memberships = this.memberships.findByUserIdOrderById(user.getId());
        var authorities = memberships.stream()
                .map(m -> new SimpleGrantedAuthority("ROLE_" + m.getRole().name()))
                .distinct()
                .toList();
        Long crecheId = memberships.isEmpty() ? null : memberships.get(0).getCrecheId();
        return new GroveUserDetails(user, crecheId, authorities);
    }
}
