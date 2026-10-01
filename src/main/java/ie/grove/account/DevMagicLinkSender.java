package ie.grove.account;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.authentication.ott.RedirectOneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Dev magic-link sender (CLAUDE.md demo logistics: dev prints links to console
 * instead of sending mail). Logs the full {@code /login/ott?token=...} URL —
 * that value exists only here and in the console, never in the database — and
 * redirects the browser to {@code /login/link?sent} so the response is the
 * same whether or not the email belongs to an account. Production upgrade
 * path: swap this bean for a Spring Mail implementation.
 */
public class DevMagicLinkSender implements OneTimeTokenGenerationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(DevMagicLinkSender.class);

    private final OneTimeTokenGenerationSuccessHandler redirect =
            new RedirectOneTimeTokenGenerationSuccessHandler("/login/link?sent");

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            OneTimeToken oneTimeToken) throws IOException, ServletException {
        String link = UriComponentsBuilder
                .fromUriString(UrlUtils.buildFullRequestUrl(request))
                .replacePath(request.getContextPath())
                .replaceQuery(null)
                .fragment(null)
                .path("/login/ott")
                .queryParam("token", oneTimeToken.getTokenValue())
                .toUriString();
        log.info("Magic link for {} (dev only, not emailed): {}", oneTimeToken.getUsername(), link);
        redirect.handle(request, response, oneTimeToken);
    }
}
