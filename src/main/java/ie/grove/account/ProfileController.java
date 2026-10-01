package ie.grove.account;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Profile surface (spec.md §4.5 — passkey management only for this chunk;
 * name/email/password edits are later). Lists the signed-in user's passkeys;
 * adding one is POST /webauthn/register, removing one is DELETE
 * /webauthn/register/{id} (both Spring Security endpoints, the delete
 * owner-checked against the current user).
 */
@Controller
public class ProfileController {

  private final AppUserRepository users;
  private final UserCredentialRepository credentials;

  public ProfileController(AppUserRepository users, UserCredentialRepository credentials) {
    this.users = users;
    this.credentials = credentials;
  }

  @GetMapping("/admin/profile")
  String profile(Authentication auth, Model model) {
    AppUser user = users.findByEmail(auth.getName()).orElseThrow();
    List<Passkey> passkeys =
        credentials.findByUserId(AppUserWebAuthnUserEntityRepository.userIdBytes(user.getId()))
            .stream()
            .map(ProfileController::toView)
            .toList();
    model.addAttribute("name", user.getName());
    model.addAttribute("email", user.getEmail());
    model.addAttribute("passkeys", passkeys);
    model.addAttribute("webauthnEnabled", true);
    return "profile";
  }

  private static Passkey toView(CredentialRecord record) {
    return new Passkey(record.getCredentialId().toBase64UrlString(), record.getLabel(),
        record.getCreated());
  }

  /** Row shape for the profile template: credential id for the delete call. */
  public record Passkey(String credentialId, String label, Instant created) {
  }
}
