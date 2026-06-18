package net.openid.conformance.ui;

import net.openid.conformance.security.AuthenticationFacade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;
import java.util.Arrays;

/**
 * Pins the auth-aware home routing contract ({@link HomeController}): anonymous
 * visitors land on the login page, authenticated users land on the plans home,
 * and the redirect stays temporary so browsers never cache the auth decision.
 *
 * Pure unit test by repo convention — there is no MockMvc/@SpringBootTest
 * infrastructure (see the header of frontend/e2e/plans-url-compat.spec.js), so
 * the branch logic is pinned here and the full-stack behaviour is covered by
 * live smoke against a non-dev profile.
 */
@ExtendWith(MockitoExtension.class)
public class HomeController_UnitTest {

	@Mock
	private AuthenticationFacade authenticationFacade;

	@InjectMocks
	private HomeController controller;

	@Test
	public void authenticated_user_is_redirected_to_the_plans_home() {
		// isAdmin() is intentionally not stubbed: isUser() short-circuits the OR.
		Mockito.when(authenticationFacade.isUser()).thenReturn(true);

		ResponseEntity<Void> response = controller.home();

		Assertions.assertEquals(HttpStatus.FOUND, response.getStatusCode());
		Assertions.assertEquals(URI.create("/plans.html"), response.getHeaders().getLocation());
	}

	@Test
	public void admin_authority_alone_is_redirected_to_the_plans_home() {
		// Proves the OR's second operand on its own. (Note: the dev-profile dummy
		// admin carries BOTH ROLE_USER and ROLE_ADMIN — see DummyUserFilter — so
		// this mock shape pins the branch logic, not a real session fixture.)
		Mockito.when(authenticationFacade.isUser()).thenReturn(false);
		Mockito.when(authenticationFacade.isAdmin()).thenReturn(true);

		ResponseEntity<Void> response = controller.home();

		Assertions.assertEquals(HttpStatus.FOUND, response.getStatusCode());
		Assertions.assertEquals(URI.create("/plans.html"), response.getHeaders().getLocation());
	}

	@Test
	public void anonymous_visitor_is_redirected_to_the_login_page() {
		Mockito.when(authenticationFacade.isUser()).thenReturn(false);
		Mockito.when(authenticationFacade.isAdmin()).thenReturn(false);

		ResponseEntity<Void> response = controller.home();

		// FOUND (302) keeps the redirect temporary — a 301 would be cached by
		// browsers and keep redirecting a previously-anonymous visitor to
		// login.html after they authenticate.
		Assertions.assertEquals(HttpStatus.FOUND, response.getStatusCode());
		Assertions.assertEquals(URI.create("/login.html"), response.getHeaders().getLocation());
	}

	@Test
	public void private_link_guest_falls_to_the_login_branch() {
		// In production a private-link session never reaches this controller for
		// "/" — the denyAll matcher in WebSecurityOidcLoginConfig is registered
		// first. This pins the defensive posture: a guest carries neither
		// ROLE_USER nor ROLE_ADMIN, so even if the chain ordering ever changed,
		// the controller must not route a guest toward the listing pages.
		// (isPrivateLinkUser() is deliberately not consulted by the controller.)
		Mockito.when(authenticationFacade.isUser()).thenReturn(false);
		Mockito.when(authenticationFacade.isAdmin()).thenReturn(false);

		ResponseEntity<Void> response = controller.home();

		Assertions.assertEquals(URI.create("/login.html"), response.getHeaders().getLocation());
		// Executable pin of the comment above: the routing decision must not
		// consult the private-link flag (guests fall through the role checks).
		Mockito.verify(authenticationFacade, Mockito.never()).isPrivateLinkUser();
	}

	@Test
	public void root_and_legacy_index_html_share_the_same_handler() throws NoSuchMethodException {
		GetMapping mapping = HomeController.class.getDeclaredMethod("home").getAnnotation(GetMapping.class);

		Assertions.assertNotNull(mapping, "home() must carry the @GetMapping annotation");
		Assertions.assertTrue(Arrays.asList(mapping.value()).containsAll(Arrays.asList("/", "/index.html")),
			"Both / and /index.html must map to the same auth-aware handler");
	}
}
