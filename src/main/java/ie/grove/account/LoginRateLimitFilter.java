package ie.grove.account;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Throttles sign-in endpoints per email+IP (spec.md §5 hardening, demo-grade):
 * guards POST /login and POST /login/link with the same in-memory window, and
 * on the counted paths (magic-link generation) every request eats budget, so
 * a mailbox can get at most {@link LoginAttemptService}'s threshold of links
 * per window. Blocked and normal failures both land on {@code /login?error} —
 * the page shows the same message whether or not the account exists.
 * Instantiated inside {@link SecurityConfig} only — must not double-register
 * as a servlet bean.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginAttemptService attempts;

    /** POST paths where each request counts as an attempt against the window. */
    private final Set<String> countedPaths;

    public LoginRateLimitFilter(LoginAttemptService attempts) {
        this(attempts, Set.of());
    }

    public LoginRateLimitFilter(LoginAttemptService attempts, Set<String> countedPaths) {
        this.attempts = attempts;
        this.countedPaths = countedPaths;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // requestURI, not servletPath: MockMvc leaves servletPath empty.
        String path = request.getRequestURI();
        boolean guarded = "/login".equals(path) || "/login/link".equals(path);
        return !"POST".equals(request.getMethod()) || !guarded;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String email = request.getParameter("username");
        String ip = LoginAttemptService.clientIp(request);
        if (attempts.isBlocked(email, ip)) {
            response.sendRedirect("/login?error");
            return;
        }
        if (countedPaths.contains(request.getRequestURI())) {
            // Generation eats budget after the gate, so the first
            // MAX_FAILURES links per window go out and the next is blocked.
            attempts.recordFailure(email, ip);
        }
        chain.doFilter(request, response);
    }
}
