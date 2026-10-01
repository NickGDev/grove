package ie.grove.account;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Component;

/**
 * Maps WebAuthn user entities onto app_user: the WebAuthn user id is the
 * app_user id as an 8-byte big-endian array, name is the email, displayName
 * the user's name. No table of its own — the identity already lives in
 * app_user, so save/delete are no-ops and a user entity exists for exactly
 * the active accounts. Removing a passkey goes through the credential
 * repository (Spring Security's DELETE /webauthn/register/&#123;id&#125;);
 * users themselves are deactivated, never deleted (spec.md §4.7).
 */
@Component
public class AppUserWebAuthnUserEntityRepository implements PublicKeyCredentialUserEntityRepository {

    private final AppUserRepository users;

    public AppUserWebAuthnUserEntityRepository(AppUserRepository users) {
        this.users = users;
    }

    /** app_user.id as the WebAuthn user id (big-endian long). */
    static Bytes userIdBytes(Long userId) {
        return new Bytes(ByteBuffer.allocate(Long.BYTES).putLong(userId).array());
    }

    static Optional<Long> bytesToUserId(Bytes id) {
        byte[] raw = id.getBytes();
        if (raw.length != Long.BYTES) {
            return Optional.empty(); // not an id we minted
        }
        return Optional.of(ByteBuffer.wrap(raw).getLong());
    }

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        Optional<Long> userId = bytesToUserId(id);
        if (userId.isEmpty()) {
            return null;
        }
        return users.findById(userId.get())
                .filter(AppUserWebAuthnUserEntityRepository::active)
                .map(this::toEntity)
                .orElse(null);
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        if (username == null) {
            return null;
        }
        return users.findByEmail(username.trim().toLowerCase())
                .filter(AppUserWebAuthnUserEntityRepository::active)
                .map(this::toEntity)
                .orElse(null);
    }

    @Override
    public void save(PublicKeyCredentialUserEntity entity) {
        // No-op: entity = app_user row, already persisted.
    }

    @Override
    public void delete(Bytes id) {
        // No-op: users are deactivated, never deleted (spec.md §4.7).
    }

    private static boolean active(AppUser user) {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    private PublicKeyCredentialUserEntity toEntity(AppUser user) {
        return ImmutablePublicKeyCredentialUserEntity.builder()
                .id(userIdBytes(user.getId()))
                .name(user.getEmail())
                .displayName(user.getName())
                .build();
    }
}
