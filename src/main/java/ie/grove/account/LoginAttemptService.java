package ie.grove.account;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;

/**
 * In-memory login throttle per email+IP (spec.md §5 hardening, demo-grade):
 * a fixed window of allowed failures before the pair is blocked. Blocked and
 * normal failures both land on {@code /login?error} — the page shows the same
 * message whether or not the account exists.
 */
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Window> failures = new ConcurrentHashMap<>();

    public boolean isBlocked(String email, String ip) {
        Window window = failures.get(key(email, ip));
        return window != null && window.active() && window.count >= MAX_FAILURES;
    }

    public void recordFailure(String email, String ip) {
        String key = key(email, ip);
        failures.compute(key, (k, existing) ->
                existing != null && existing.active()
                        ? new Window(existing.start, existing.count + 1)
                        : new Window(System.currentTimeMillis(), 1));
    }

    public void recordSuccess(String email, String ip) {
        failures.remove(key(email, ip));
    }

    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String key(String email, String ip) {
        return (email == null ? "" : email.trim().toLowerCase()) + "|" + ip;
    }

    private record Window(long start, int count) {
        boolean active() {
            return System.currentTimeMillis() - start < WINDOW.toMillis();
        }
    }
}
