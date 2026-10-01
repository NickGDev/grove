package ie.grove.account;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

/**
 * Passkey credential storage: Spring Security's JDBC repository runs as-is on
 * SQLite (its byte params are base64url text, blobs round-trip through
 * xerial) against the table owned by V3__passwordless.sql. The user-entity
 * half is {@link AppUserWebAuthnUserEntityRepository}, so credentials map to
 * app_user rows.
 */
@Configuration
public class WebAuthnConfig {

  @Bean
  UserCredentialRepository userCredentialRepository(JdbcOperations jdbc) {
    return new JdbcUserCredentialRepository(jdbc);
  }
}
