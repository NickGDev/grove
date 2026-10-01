package ie.grove.account;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.ott.DefaultOneTimeToken;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.stereotype.Component;

/**
 * SQLite-backed one-time tokens (spec.md §5 hardening): the value in the
 * emailed link is random and only its SHA-256 digest is stored, links expire
 * after 10 minutes, and consuming deletes the row so a link works exactly
 * once. Replaces Spring Security's JdbcOneTimeTokenService, which stores the
 * raw token value. Table owned by V3__passwordless.sql.
 */
@Component
public class HashedOneTimeTokenService implements OneTimeTokenService {

    private static final Duration VALIDITY = Duration.ofMinutes(10);

    private final JdbcOperations jdbc;

    public HashedOneTimeTokenService(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OneTimeToken generate(GenerateOneTimeTokenRequest request) {
        // Demo-grade housekeeping: drop rows nobody will ever consume again.
        jdbc.update("delete from one_time_tokens where expires_at < ?",
                Timestamp.from(Instant.now()));
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(VALIDITY);
        jdbc.update("insert into one_time_tokens (token_value, username, expires_at) values (?, ?, ?)",
                sha256(token), request.getUsername(), Timestamp.from(expiresAt));
        return new DefaultOneTimeToken(token, request.getUsername(), expiresAt);
    }

    @Override
    public OneTimeToken consume(OneTimeTokenAuthenticationToken authenticationToken) {
        String digest = sha256(authenticationToken.getTokenValue());
        try {
            OneTimeToken token = jdbc.queryForObject(
                    "select token_value, username, expires_at from one_time_tokens where token_value = ?",
                    (rs, i) -> new DefaultOneTimeToken(rs.getString(1), rs.getString(2),
                            rs.getTimestamp(3).toInstant()),
                    digest);
            if (token != null && token.getExpiresAt().isAfter(Instant.now())) {
                return token;
            }
            return null;
        } catch (EmptyResultDataAccessException unknownToken) {
            return null;
        } finally {
            // Single use: the row dies whether or not the token was valid.
            jdbc.update("delete from one_time_tokens where token_value = ?", digest);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
