package ie.grove.account;

import java.time.LocalDateTime;

import ie.grove.creche.CrecheRepository;
import ie.grove.shared.TenantContext;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * docs/spec.md §4-§5: signup, form login, role walls, tenant context plumbing.
 * Runs against target/test.db (V1 schema + V2 demo seed). Emails get a run
 * suffix so reruns never collide with rows from an earlier run.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountSecurityTests {

  private static final String SAME_ERROR = "That email and password didn't match";

  @Autowired MockMvc mvc;
  @Autowired SignupService signupService;
  @Autowired GroveUserDetailsService userDetailsService;
  @Autowired AppUserRepository users;
  @Autowired MembershipRepository memberships;
  @Autowired CrecheRepository creches;
  @Autowired PasswordEncoder encoder;

  @AfterEach
  void clearContext() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  // --- signup ---

  @Test
  void signupCreatesCrecheAndOwnerThenLogsIn() throws Exception {
    String email = "owner" + suffix() + "@test.ie";
    MvcResult result = mvc.perform(post("/signup").with(csrf())
            .param("email", email).param("name", "Nora Kelly")
            .param("password", "long-enough-pass-12").param("crecheName", "Kelly's Corner"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/home"))
        .andReturn();
    MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
    assertNotNull(session);

    AppUser user = users.findByEmail(email).orElseThrow();
    assertEquals("Nora Kelly", user.getName());
    var membershipsForUser = memberships.findByUserIdOrderById(user.getId());
    assertEquals(1, membershipsForUser.size());
    assertEquals(Role.OWNER, membershipsForUser.get(0).getRole());
    Long crecheId = membershipsForUser.get(0).getCrecheId();
    assertNotNull(creches.findById(crecheId).orElseThrow());

    // Fresh login with the same credentials also reaches the admin surface.
    MvcResult login = mvc.perform(post("/login").with(csrf())
            .param("username", email).param("password", "long-enough-pass-12"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/home"))
        .andReturn();
    mvc.perform(get("/home").session((MockHttpSession) login.getRequest().getSession(false)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin"));
    mvc.perform(get("/admin").session((MockHttpSession) login.getRequest().getSession(false)))
        .andExpect(status().isOk());
  }

  @Test
  void signupRejectsShortPasswordWithoutCreatingAnything() throws Exception {
    String email = "shortpw" + suffix() + "@test.ie";
    int crechesBefore = creches.findAll().size();
    // No status assertion: the "signup" view render is the frontend owner's
    // signup.html (pending) — this test pins the data contract only.
    mvc.perform(post("/signup").with(csrf())
            .param("email", email).param("name", "Shorty")
            .param("password", "too-short").param("crecheName", "Nope Creche"))
        .andReturn();
    assertEquals(crechesBefore, creches.findAll().size());
    assertNull(users.findByEmail(email).orElse(null));
  }

  @Test
  void signupDuplicateEmailShowsError() throws Exception {
    var controller = new SignupController(signupService, userDetailsService);
    // Stub view render: the real signup.html belongs to the frontend owner.
    var standalone = org.springframework.test.web.servlet.setup.MockMvcBuilders
        .standaloneSetup(controller)
        .setSingleView(new org.springframework.web.servlet.view.AbstractView() {
          @Override protected void renderMergedOutputModel(java.util.Map<String, Object> model,
              jakarta.servlet.http.HttpServletRequest request,
              jakarta.servlet.http.HttpServletResponse response) { }
        })
        .build();
    standalone.perform(post("/signup")
            .param("email", "demo@grove.ie").param("name", "Dup")
            .param("password", "long-enough-pass-12").param("crecheName", "Dup Creche"))
        .andExpect(view().name("signup"))
        .andExpect(model().attribute("error", "That email is already in use. Try signing in instead."));

    standalone.perform(post("/signup")
            .param("email", "not-an-email").param("name", "Dup")
            .param("password", "long-enough-pass-12").param("crecheName", "Dup Creche"))
        .andExpect(view().name("signup"))
        .andExpect(model().attribute("error", "Enter a valid email address."));
  }

  // --- login ---

  @Test
  void wrongPasswordShowsSameMessageForKnownAndUnknownEmail() throws Exception {
    mvc.perform(post("/login").with(csrf())
            .param("username", "demo@grove.ie").param("password", "definitely-wrong"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
    mvc.perform(get("/login").param("error", ""))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString(SAME_ERROR)));

    mvc.perform(post("/login").with(csrf())
            .param("username", "nobody+" + suffix() + "@grove.ie").param("password", "definitely-wrong"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
    mvc.perform(get("/login").param("error", ""))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString(SAME_ERROR)));
  }

  // --- role walls ---

  @Test
  void parentIsRedirectedToPortalAndBlockedFromAdmin() throws Exception {
    String email = fixture("PARENT" + suffix() + "@test.ie", Role.PARENT);
    MockHttpSession session = login(email, "parent-pass-12");
    mvc.perform(get("/home").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/portal"));
    mvc.perform(get("/admin").session(session))
        .andExpect(status().isForbidden());
  }

  @Test
  void staffReachesAdmin() throws Exception {
    String email = fixture("STAFF" + suffix() + "@test.ie", Role.STAFF);
    mvc.perform(get("/admin").session(login(email, "staff-pass-12")))
        .andExpect(status().isOk());
  }

  @Test
  void anonymousIsRedirectedToLogin() throws Exception {
    mvc.perform(get("/admin"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void seededDemoOwnerLogsIn() throws Exception {
    MockHttpSession session = login("demo@grove.ie", "grove");
    mvc.perform(get("/home").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin"));
    mvc.perform(get("/admin").session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard")));
  }

  // --- tenant context ---

  @Test
  void tenantFilterSetsContextInsideChainAndClearsAfter() throws Exception {
    var details = (GroveUserDetails) userDetailsService.loadUserByUsername("demo@grove.ie");
    assertEquals(1L, details.getCrecheId());
    SecurityContextHolder.getContext()
        .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            details, null, details.getAuthorities()));

    var request = new MockHttpServletRequest("GET", "/admin");
    var response = new MockHttpServletResponse();
    var seen = new Long[1];
    jakarta.servlet.FilterChain chain = (req, res) -> seen[0] = TenantContext.get();

    new TenantContextFilter().doFilter(request, response, chain);

    assertEquals(1L, seen[0]);
    assertNull(TenantContext.get());
  }

  @Test
  void tenantFilterLeavesNoContextAfterDeniedRequest() throws Exception {
    mvc.perform(get("/admin").with(user("anon@test.ie").roles("PARENT")))
        .andExpect(status().isForbidden());
    assertNull(TenantContext.get());
  }

  // --- helpers ---

  private static String suffix() {
    return Long.toString(System.nanoTime());
  }

  private String fixture(String email, Role role) {
    AppUser user = new AppUser(email.toLowerCase(), "Test " + role, LocalDateTime.of(2026, 9, 1, 9, 0));
    user.setPasswordHash(encoder.encode(role.name().toLowerCase() + "-pass-12"));
    users.save(user);
    Long previous = TenantContext.get();
    TenantContext.set(1L); // seeded Grove Family Creche
    try {
      memberships.saveAndFlush(new Membership(user.getId(), role));
    } finally {
      if (previous != null) {
        TenantContext.set(previous);
      } else {
        TenantContext.clear();
      }
    }
    return user.getEmail();
  }

  private MockHttpSession login(String email, String password) throws Exception {
    MvcResult result = mvc.perform(post("/login").with(csrf())
            .param("username", email).param("password", password))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", "/home"))
        .andReturn();
    HttpSession session = result.getRequest().getSession(false);
    assertNotNull(session);
    return (MockHttpSession) session;
  }
}
