package ie.grove.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Creche sign-up: email, name, password (min 12), creche name — creates the
 * creche + OWNER, signs the new owner in, lands on /home. Template contract:
 * view "signup" with an optional {@code error} model attribute on redisplay.
 */
@Controller
public class SignupController {

  private final SignupService signup;
  private final GroveUserDetailsService users;
  private final SecurityContextRepository securityContext =
      new HttpSessionSecurityContextRepository();

  public SignupController(SignupService signup, GroveUserDetailsService users) {
    this.signup = signup;
    this.users = users;
  }

  @GetMapping("/signup")
  String signup() {
    return "signup";
  }

  @PostMapping("/signup")
  String signup(@RequestParam(required = false) String email,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String password,
      @RequestParam(required = false) String crecheName,
      HttpServletRequest request, HttpServletResponse response, Model model) {

    String error = validate(email, name, password, crecheName);
    if (error == null) {
      String normalizedEmail = email.trim().toLowerCase();
      try {
        signup.signup(normalizedEmail, name.trim(), password, crecheName.trim());
        authenticate(normalizedEmail, password, request, response);
        return "redirect:/home";
      } catch (SignupService.EmailAlreadyInUseException e) {
        error = "That email is already in use. Try signing in instead.";
      }
    }
    model.addAttribute("error", error);
    return "signup";
  }

  private static String validate(String email, String name, String password, String crecheName) {
    if (email == null || email.isBlank()) return "Enter your email.";
    if (!email.trim().matches(".+@.+\\..+")) return "Enter a valid email address.";
    if (name == null || name.isBlank()) return "Enter your name.";
    if (crecheName == null || crecheName.isBlank()) return "Enter your creche's name.";
    if (password == null || password.length() < 12) {
      return "Password must be at least 12 characters.";
    }
    return null;
  }

  private void authenticate(String email, String password,
      HttpServletRequest request, HttpServletResponse response) {
    GroveUserDetails details = (GroveUserDetails) users.loadUserByUsername(email);
    Authentication auth = new UsernamePasswordAuthenticationToken(
        details, password, details.getAuthorities());
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    securityContext.saveContext(context, request, response);
  }
}
