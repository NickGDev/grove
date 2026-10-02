package ie.grove.account;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

  @GetMapping("/login")
  String login(Model model) {
    model.addAttribute("webauthnEnabled", true);
    return "login";
  }

  /**
   * "Email me a sign-in link instead" (docs/spec.md §4.3): the form here POSTs
   * {@code username} to /login/link — Spring Security's one-time-token
   * generation URL — and {@code ?sent} shows the check-your-inbox note the
   * generation handler redirects to. Same view whatever the email.
   */
  @GetMapping("/login/link")
  String loginLink(@RequestParam(value = "sent", required = false) String sent, Model model) {
    model.addAttribute("sent", sent != null);
    model.addAttribute("webauthnEnabled", true);
    return "login-link";
  }
}
