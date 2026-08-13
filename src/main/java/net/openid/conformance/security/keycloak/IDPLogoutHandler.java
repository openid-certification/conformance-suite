package net.openid.conformance.security.keycloak;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Terminates the local session and, for sessions established through the IdP,
 * propagates the logout to the IdP's end-session endpoint (RP-initiated logout).
 * <p>
 * This is a {@link org.springframework.security.web.authentication.logout.LogoutSuccessHandler},
 * deliberately NOT a {@code LogoutHandler}: handlers run <em>before</em> the success
 * handler, so a handler that redirects commits the response and the success handler's
 * own redirect is silently lost. Exactly one component may own the redirect, and this
 * is it — for every kind of principal.
 * <p>
 * Principals that did not come from the IdP (private-link one-time-token logins, the
 * dev-mode dummy user, an already-anonymous request, or an ID token from some other
 * issuer) fall through to the local {@link #LOCAL_LOGOUT_URL}, as does an IdP that
 * does not advertise an end-session endpoint — RP-initiated logout is optional, and
 * an OP that does not support it has nothing for us to redirect to.
 */
// final: the constructor calls the inherited setDefaultTargetUrl(), which would
// otherwise be a 'this' escape before a subclass finished initializing (-Werror).
@Component
public final class IDPLogoutHandler extends SimpleUrlLogoutSuccessHandler {

	private static final Logger log = LoggerFactory.getLogger(IDPLogoutHandler.class);

	/**
	 * `?logout=true` lights up the "You have been logged out." banner on login.html
	 * (it gates on the param VALUE being truthy, so a bare `?logout` would not
	 * trigger it). The IdP round-trip comes back here too, so the banner shows
	 * whichever path logout took.
	 */
	public static final String LOCAL_LOGOUT_URL = "/login.html?logout=true";

	/**
	 * Matches the `spring.security.oauth2.client.registration.idp.*` property
	 * namespace that defines the IdP client.
	 */
	private static final String REGISTRATION_ID = "idp";

	private static final String END_SESSION_ENDPOINT = "end_session_endpoint";

	// ObjectProvider, not the repository itself: the repository is a @Bean on
	// WebSecurityOidcLoginConfig, which in turn injects this handler, so taking it
	// directly forms a bean cycle that Spring rejects at startup. ObjectProvider
	// defers the lookup to logout time, by which point the repository is built.
	private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;

	private final String baseUrl;

	public IDPLogoutHandler(ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
							@Value("${fintechlabs.base_url}") String baseUrl) {
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.baseUrl = baseUrl;
		setDefaultTargetUrl(LOCAL_LOGOUT_URL);
	}

	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth)
		throws IOException, ServletException {

		ClientRegistration registration = idpRegistration();
		OidcIdToken idToken = idpIdToken(auth, registration);
		String endSessionEndpoint = endSessionEndpoint(registration);

		if (idToken == null || endSessionEndpoint == null) {
			// Not an IdP session, or an IdP with no RP-initiated logout support;
			// the local logout is all there is to do.
			super.onLogoutSuccess(request, response, auth);
			return;
		}

		log.info("Propagating logout to the IdP. userId={}", idToken.getSubject());
		getRedirectStrategy().sendRedirect(request, response, createIdpLogoutUrl(endSessionEndpoint, idToken));
	}

	private ClientRegistration idpRegistration() {
		ClientRegistrationRepository repository = clientRegistrationRepository.getIfAvailable();
		// Null in dev mode when discovery failed and the repository was left empty.
		return repository == null ? null : repository.findByRegistrationId(REGISTRATION_ID);
	}

	/**
	 * @return the IdP's advertised end-session endpoint, or null if the IdP is
	 * unknown or does not support RP-initiated logout. Read from the discovery
	 * document rather than derived from the issuer: OPs place it at wildly
	 * different paths (Keycloak uses {issuer}/protocol/openid-connect/logout),
	 * so deriving it silently breaks whenever the IdP changes.
	 */
	private String endSessionEndpoint(ClientRegistration registration) {
		if (registration == null) {
			return null;
		}
		Object endpoint = registration.getProviderDetails().getConfigurationMetadata().get(END_SESSION_ENDPOINT);
		return endpoint instanceof String uri && !uri.isBlank() ? uri : null;
	}

	/**
	 * @return the ID token issued by our IdP for this session, or null if the
	 * principal did not come from our IdP (including an unauthenticated request,
	 * for which the container passes a null Authentication).
	 */
	private OidcIdToken idpIdToken(Authentication auth, ClientRegistration registration) {
		if (registration == null || auth == null || !(auth.getPrincipal() instanceof OidcUser oidcUser)) {
			return null;
		}
		String idpIssuer = registration.getProviderDetails().getIssuerUri();
		OidcIdToken idToken = oidcUser.getIdToken();
		if (idToken == null || idToken.getIssuer() == null || idpIssuer == null
			|| !idToken.getIssuer().toString().equals(idpIssuer)) {
			return null;
		}
		return idToken;
	}

	private String createIdpLogoutUrl(String endSessionEndpoint, OidcIdToken idToken) {
		return UriComponentsBuilder.fromUriString(endSessionEndpoint)
			// Without id_token_hint the IdP cannot identify the RP, and will
			// ignore post_logout_redirect_uri in favour of its own confirmation page.
			.queryParam("id_token_hint", idToken.getTokenValue())
			// From configuration, not from the request's Host header: the value has
			// to match a URI registered with the IdP, and the request's host is
			// client-controlled input that a proxy may not have normalized.
			.queryParam("post_logout_redirect_uri", baseUrl + LOCAL_LOGOUT_URL)
			.build()
			.encode()
			.toUriString();
	}
}
