package ie.grove.shared;

/**
 * Request-scoped creche id. Set by the auth layer once it lands; until then
 * callers (tests, seeding) set it explicitly. Always {@link #clear()} when done.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long crecheId) {
        CURRENT.set(crecheId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
