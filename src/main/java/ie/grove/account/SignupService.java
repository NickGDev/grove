package ie.grove.account;

import java.time.LocalDateTime;

import ie.grove.creche.Creche;
import ie.grove.creche.CrecheRepository;
import ie.grove.shared.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Creche sign-up (spec.md §4.1, minus email verification — later chunk):
 * creche + owner user + OWNER membership.
 *
 * Hibernate pins the tenant on a Session when it opens, so the membership
 * insert must happen in a session opened AFTER the creche id exists. Each
 * repository call therefore runs its own short transaction (creche/user on
 * the root tenant — they carry no creche_id — membership under the new
 * creche's context). Not one atomic transaction; a failure mid-way can leave
 * a creche without its owner.
 */
@Service
public class SignupService {

    private final CrecheRepository creches;
    private final AppUserRepository users;
    private final MembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;

    public SignupService(CrecheRepository creches, AppUserRepository users,
            MembershipRepository memberships, PasswordEncoder passwordEncoder) {
        this.creches = creches;
        this.users = users;
        this.memberships = memberships;
        this.passwordEncoder = passwordEncoder;
    }

    /** @throws EmailAlreadyInUse if the email is registered. */
    public Creche signup(String email, String name, String password, String crecheName) {
        users.findByEmail(email).ifPresent(existing -> {
            throw new EmailAlreadyInUseException(email);
        });
        Creche creche = creches.save(new Creche(crecheName));
        AppUser user = new AppUser(email, name, LocalDateTime.now());
        user.setPasswordHash(passwordEncoder.encode(password));
        users.save(user);
        // Membership is a @TenantEntity: Hibernate fills creche_id from TenantContext.
        Long previous = TenantContext.get();
        TenantContext.set(creche.getId());
        try {
            memberships.saveAndFlush(new Membership(user.getId(), Role.OWNER));
        } finally {
            if (previous != null) {
                TenantContext.set(previous);
            } else {
                TenantContext.clear();
            }
        }
        return creche;
    }

    public static class EmailAlreadyInUseException extends RuntimeException {
        public EmailAlreadyInUseException(String email) {
            super("Email already in use: " + email);
        }
    }
}
