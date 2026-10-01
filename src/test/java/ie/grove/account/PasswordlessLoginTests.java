package ie.grove.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

import jakarta.servlet.http.HttpSession;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ie.grove.shared.TenantContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Passwordless login (spec.md §5): one-time-token magic links (hashed storage,
 * 10-minute expiry, single use, rate-limited generation, identical response
 * for known/unknown emails) and WebAuthn passkeys (endpoints up, credentials
 * stored on SQLite, user entities mapped to app_user). Views are the frontend
 * owner's — src/test/resources/templates holds render stubs for login-link
 * and profile only.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordlessLoginTests {

  private static final String SAME_ERROR = "That email and password didn't match";

  @Autowired MockMvc mvc;
  @Autowired HashedOneTimeTokenService oneTimeTokens;
  @Autowired UserCredentialRepository credentials;
  @Autowired AppUserRepository users;
  @Autowired JdbcTemplate jdbc;

  @AfterEach
  void clearContext() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  // --- magic link: generation ---

  @Test
  void linkGenerationRedirectsTheSameForKnownAndUnknownEmail() throws Exception {
    mvc.perform(post("/login/link").with(csrf()).param("username", "demo@grove.ie"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login/link?sent"));
    mvc.perform(post("/login/link").with(csrf())
            .param("username", "ghost+" + suffix() + "@grove.ie"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login/link?sent"));
  }

  @Test
  void loginLinkPageShowsSentNoteAndWebauthnFlag() throws Exception {
    mvc.perform(get("/login/link"))
        .andExpect(status().isOk())
        .andExpect(view().name("login-link"))
        .andExpect(content().string(Matchers.not(Matchers.containsString("check-your-inbox"))));
    mvc.perform(get("/login/link").param("sent", ""))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.allOf(
            Matchers.containsString("check-your-inbox"),
            Matchers.containsString("true"))));
  }

  @Test
  void generatedTokenIsStoredHashedNotRaw() {
    String token = oneTimeTokens
        .generate(new GenerateOneTimeTokenRequest("demo@grove.ie")).getTokenValue();
    assertEquals(0, countTokens(token), "raw token must never be stored");
    assertEquals(1, countTokens(sha256Hex(token)));
  }

  // --- magic link: consume ---

  @Test
  void magicLinkSignsInThenIsSingleUse() throws Exception {
    String token = oneTimeTokens
        .generate(new GenerateOneTimeTokenRequest("demo@grove.ie")).getTokenValue();

    MvcResult result = mvc.perform(post("/login/ott").with(csrf()).param("token", token))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/home"))
        .andReturn();
    HttpSession session = result.getRequest().getSession(false);
    assertNotNull(session);
    mvc.perform(get("/home").session((MockHttpSession) session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin"));

    // Second use: consumed rows are gone, so the link lands on the generic error.
    mvc.perform(post("/login/ott").with(csrf()).param("token", token))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
    mvc.perform(get("/login").param("error", ""))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(SAME_ERROR)));
  }

  @Test
  void expiredAndUnknownTokensGetTheGenericError() throws Exception {
    jdbc.update("insert into one_time_tokens (token_value, username, expires_at) values (?, ?, ?)",
        sha256Hex("stale-token"), "demo@grove.ie",
        java.sql.Timestamp.from(Instant.now().minus(Duration.ofMinutes(1))));

    mvc.perform(post("/login/ott").with(csrf()).param("token", "stale-token"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
    mvc.perform(post("/login/ott").with(csrf()).param("token", "never-existed"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
  }

  // --- magic link: rate limit ---

  @Test
  void linkGenerationIsRateLimitedPerEmail() throws Exception {
    String email = "linkflooder+" + suffix() + "@test.ie";
    for (int i = 0; i < 5; i++) {
      mvc.perform(post("/login/link").with(csrf()).param("username", email))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/login/link?sent"));
    }
    mvc.perform(post("/login/link").with(csrf()).param("username", email))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
  }

  // --- passkeys: profile page ---

  @Test
  void profileRequiresSignIn() throws Exception {
    mvc.perform(get("/admin/profile"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void profileListsPasskeysForTheSignedInUser() throws Exception {
    AppUser demo = users.findByEmail("demo@grove.ie").orElseThrow();
    insertCredential(demo.getId(), "demo yubikey", "demo-cred-" + suffix());

    MockHttpSession session = login("demo@grove.ie", "grove");
    mvc.perform(get("/admin/profile").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("profile"))
        .andExpect(content().string(Matchers.allOf(
            Matchers.containsString("demo yubikey"),
            Matchers.containsString("true"),
            Matchers.not(Matchers.containsString("other yubikey")))));
  }

  // --- passkeys: endpoints ---

  @Test
  void registrationOptionsUseTheAppUserMappedWebAuthnId() throws Exception {
    AppUser demo = users.findByEmail("demo@grove.ie").orElseThrow();
    insertCredential(demo.getId(), "demo yubikey", "demo-cred-" + suffix());
    String expectedUserId = AppUserWebAuthnUserEntityRepository.userIdBytes(demo.getId())
        .toBase64UrlString();

    MockHttpSession session = login("demo@grove.ie", "grove");
    mvc.perform(post("/webauthn/register/options").session(session).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.allOf(
            Matchers.containsString(expectedUserId),
            Matchers.containsString("Grove"))));
  }

  @Test
  void registerOptionsSelfGuardAndAuthenticateOptionsArePublic() throws Exception {
    // The filter hides itself from unauthenticated clients instead of advertising.
    mvc.perform(post("/webauthn/register/options").with(csrf()))
        .andExpect(status().is4xxClientError());
    mvc.perform(post("/webauthn/authenticate/options").with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void passkeyDeleteIsOwnerChecked() throws Exception {
    AppUser demo = users.findByEmail("demo@grove.ie").orElseThrow();
    String mine = "demo-cred-" + suffix();
    insertCredential(demo.getId(), "demo yubikey", mine);

    MockHttpSession session = login("demo@grove.ie", "grove");
    mvc.perform(delete("/webauthn/register/{id}", new Bytes(mine.getBytes(StandardCharsets.UTF_8)).toBase64UrlString())
            .session(session).with(csrf()))
        .andExpect(status().is2xxSuccessful());
    assertEquals(0, countCredentials(mine));

    // Another user entity's credential: authenticated but not the owner.
    String theirs = "other-cred-" + suffix();
    insertCredential(demo.getId() + 1, "other yubikey", theirs);
    mvc.perform(delete("/webauthn/register/{id}", new Bytes(theirs.getBytes(StandardCharsets.UTF_8)).toBase64UrlString())
            .session(session).with(csrf()))
        .andExpect(status().isForbidden());
    assertEquals(1, countCredentials(theirs));
  }

  // --- helpers ---

  private void insertCredential(Long appUserId, String label, String credentialId) {
    Bytes userEntityUserId = AppUserWebAuthnUserEntityRepository.userIdBytes(appUserId);
    Bytes credId = new Bytes(credentialId.getBytes(StandardCharsets.UTF_8));
    CredentialRecord record = ImmutableCredentialRecord.builder()
        .credentialType(PublicKeyCredentialType.PUBLIC_KEY)
        .credentialId(credId)
        .userEntityUserId(userEntityUserId)
        .publicKey(new ImmutablePublicKeyCose(new byte[] {1, 2, 3, 4}))
        .signatureCount(0)
        .uvInitialized(true)
        .transports(Set.of(AuthenticatorTransport.INTERNAL, AuthenticatorTransport.HYBRID))
        .backupEligible(true)
        .backupState(false)
        .attestationObject(new Bytes(new byte[] {5, 6, 7, 8}))
        .attestationClientDataJSON(new Bytes(new byte[] {9, 10, 11, 12}))
        .created(Instant.now())
        .lastUsed(Instant.now())
        .label(label)
        .build();
    credentials.save(record);
  }

  private int countTokens(String tokenValue) {
    return jdbc.queryForObject(
        "select count(*) from one_time_tokens where token_value = ?", Integer.class, tokenValue);
  }

  private int countCredentials(String credentialId) {
    String base64Url = new Bytes(credentialId.getBytes(StandardCharsets.UTF_8)).toBase64UrlString();
    return jdbc.queryForObject(
        "select count(*) from user_credentials where credential_id = ?", Integer.class, base64Url);
  }

  private static String sha256Hex(String value) {
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String suffix() {
    return Long.toString(System.nanoTime());
  }

  private MockHttpSession login(String email, String password) throws Exception {
    MvcResult result = mvc.perform(post("/login").with(csrf())
            .param("username", email).param("password", password))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/home"))
        .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
  }
}
