package ie.grove.shared;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Routes /home by role after login (spec.md §5): parents to the portal,
 * everyone with a staff-side role to admin. A PARENT who is also staff goes
 * to admin. Multi-membership users get a creche picker in a later chunk.
 */
@Controller
public class HomeController {

  private static final Set<String> STAFF_ROLES =
      Set.of("ROLE_OWNER", "ROLE_MANAGER", "ROLE_STAFF");

  @GetMapping("/home")
  String home(Authentication auth) {
    boolean staff = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(STAFF_ROLES::contains);
    return staff ? "redirect:/admin" : "redirect:/portal";
  }
}
