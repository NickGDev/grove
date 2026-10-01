package ie.grove.account;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Role walls per spec.md §5. Login is three-factor-optional: password form,
 * one-time-token magic link (POST /login/link generates; the emailed
 * /login/ott?token=... URL consumes) and WebAuthn passkeys on rpId localhost
 * (demo origin). Remember-me and invites are later chunks; login rate limiting
 * is in-memory (demo-grade) and every failure path lands on
 * {@code /login?error} so the page never reveals account existence.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    // Delegating: stores carry a {bcrypt}-style id, default strength for bcrypt.
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, HashedOneTimeTokenService oneTimeTokens) throws Exception {
    LoginAttemptService attempts = new LoginAttemptService();
    return http
      .authorizeHttpRequests(a -> a
        .requestMatchers("/login/**", "/signup/**", "/css/**", "/js/**", "/favicon.ico",
                         "/manifest.webmanifest", "/sw.js").permitAll()
        .requestMatchers("/admin/**").hasAnyRole("OWNER", "MANAGER", "STAFF")
        .requestMatchers("/portal/**").hasRole("PARENT")
        .anyRequest().authenticated())
      .formLogin(f -> f
        .loginPage("/login")
        .successHandler(successHandler(attempts))
        .failureHandler(failureHandler(attempts)))
      .oneTimeTokenLogin(o -> o
        // Same page as the password form, so Spring's generated login page
        // stays off and /login?error keeps rendering login.html. The processing
        // URL must be pinned too: loginPage() silently defaults it to /login.
        .loginPage("/login")
        .loginProcessingUrl("/login/ott")
        .tokenGeneratingUrl("/login/link")
        .tokenService(oneTimeTokens)
        .tokenGenerationSuccessHandler(new DevMagicLinkSender())
        .successHandler(ottSuccessHandler(attempts)))
      .webAuthn(w -> w
        .rpName("Grove")
        .rpId("localhost")
        .allowedOrigins("http://localhost:8080", "http://localhost"))
      .headers(h -> {
        h.contentSecurityPolicy(csp -> csp.policyDirectives(
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
            "font-src 'self' https://fonts.gstatic.com; " +
            "img-src 'self' data:; " +
            "connect-src 'self'"));
        h.permissionsPolicy(p -> p.policy("camera=(self), microphone=(), geolocation=()"));
        h.referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
      })
      .logout(l -> l.logoutSuccessUrl("/login?logout"))
      // Ahead of the OTT generate filter, so throttled mailboxes never reach it.
      .addFilterBefore(new LoginRateLimitFilter(attempts, Set.of("/login/link")),
          org.springframework.security.web.authentication.ott.GenerateOneTimeTokenFilter.class)
      .addFilterBefore(new TenantContextFilter(), AuthorizationFilter.class)
      .build();
  }

  private static org.springframework.security.web.authentication.AuthenticationSuccessHandler successHandler(
      LoginAttemptService attempts) {
    return (HttpServletRequest request, HttpServletResponse response,
            org.springframework.security.core.Authentication authentication) -> {
      attempts.recordSuccess(request.getParameter("username"), LoginAttemptService.clientIp(request));
      response.sendRedirect("/home");
    };
  }

  private static org.springframework.security.web.authentication.AuthenticationSuccessHandler ottSuccessHandler(
      LoginAttemptService attempts) {
    return (HttpServletRequest request, HttpServletResponse response,
            org.springframework.security.core.Authentication authentication) -> {
      attempts.recordSuccess(authentication.getName(), LoginAttemptService.clientIp(request));
      response.sendRedirect("/home");
    };
  }

  private static AuthenticationFailureHandler failureHandler(LoginAttemptService attempts) {
    return (HttpServletRequest request, HttpServletResponse response,
            org.springframework.security.core.AuthenticationException exception) -> {
      attempts.recordFailure(request.getParameter("username"), LoginAttemptService.clientIp(request));
      response.sendRedirect("/login?error");
    };
  }
}
