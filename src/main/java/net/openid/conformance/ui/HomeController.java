package net.openid.conformance.ui;

import net.openid.conformance.security.AuthenticationFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

/**
 * Auth-aware home routing: {@code /} and the legacy {@code /index.html} resolve to the
 * plans home for authenticated users and to the login page for anonymous visitors, via
 * a server-side temporary redirect.
 *
 * <p>This replaces the unconditional {@code addViewControllers} redirects (formerly in
 * {@code ApplicationConfig}) that sent everyone to {@code /plans.html}. Logged-out
 * visitors land on the login page instead, which carries "browse without signing in"
 * links into the public {@code ?public=true} listing views. Existing bookmarks of
 * {@code /} and {@code /index.html} keep resolving, and the OIDC/OTT login flows keep
 * working, because both paths still answer with a redirect rather than disappearing.</p>
 *
 * <p>Load-bearing details:</p>
 * <ul>
 * <li>OAuth2 login uses Spring's default {@code SavedRequestAwareAuthenticationSuccessHandler}
 * (no custom {@code successHandler} is configured in {@code WebSecurityOidcLoginConfig}),
 * whose no-saved-request fallback target is {@code /}. A freshly-authenticated user
 * therefore hits this controller immediately after login and must be routed to
 * {@code /plans.html} — keep this coupling in mind if a custom success handler is ever
 * added.</li>
 * <li>The redirect must stay temporary (302/303, never 301). A permanent redirect would
 * be cached by browsers and keep sending a previously-anonymous visitor to
 * {@code /login.html} after they authenticate.</li>
 * <li>Private-link (OTT guest) sessions never reach this controller for {@code /}: the
 * private-link denyAll matcher in {@code WebSecurityOidcLoginConfig} is registered before
 * the listing permitAll block and first-match-wins. Defensively, such sessions carry
 * neither {@code ROLE_USER} nor {@code ROLE_ADMIN}, so they fall to the login branch
 * rather than being routed toward listing pages their session may not see.</li>
 * <li>The redirect targets are hardcoded literals on purpose. Never derive the target
 * from request input (e.g. a {@code next}/{@code redirect} query parameter) — that would
 * open an open-redirect vector on the site root.</li>
 * </ul>
 */
@Controller
public class HomeController {

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@GetMapping({"/", "/index.html"})
	public ResponseEntity<Void> home() {
		boolean authenticated = authenticationFacade.isUser() || authenticationFacade.isAdmin();
		String target = authenticated ? "/plans.html" : "/login.html";
		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
	}
}
