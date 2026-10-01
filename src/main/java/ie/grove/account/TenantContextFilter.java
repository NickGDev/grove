package ie.grove.account;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import ie.grove.shared.TenantContext;

/**
 * Resolves the signed-in user's creche into {@link TenantContext} before
 * controllers run and clears it afterwards (spec.md §6 — every query scoped
 * to one creche). Instantiated inside {@link SecurityConfig} only — must not
 * double-register as a servlet bean.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !(auth instanceof AnonymousAuthenticationToken)
                    && auth.getPrincipal() instanceof GroveUserDetails details
                    && details.getCrecheId() != null) {
                TenantContext.set(details.getCrecheId());
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
