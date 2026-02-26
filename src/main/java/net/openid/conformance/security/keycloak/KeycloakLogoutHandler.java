package net.openid.conformance.security.keycloak;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class KeycloakLogoutHandler implements LogoutHandler {

	private static final Logger log = LoggerFactory.getLogger(KeycloakLogoutHandler.class);

	private final String keycloakIssuerUri;

	public KeycloakLogoutHandler(@Value("${spring.security.oauth2.client.provider.idp.issuerUri}") String keycloakIssuerUri) {
		this.keycloakIssuerUri = keycloakIssuerUri;
	}

	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response, Authentication auth) {

		var principal = (DefaultOidcUser) auth.getPrincipal();
		var idToken = principal.getIdToken();

		log.info("Propagate logout to keycloak for user. userId={}", idToken.getSubject());

		var issuerUri = idToken.getIssuer().toString();
		// If the issuerUri is not our Keycloak IdP, skip the logout propagation
		if (!issuerUri.equals(keycloakIssuerUri)) {
			try {
				request.logout();
			} catch (ServletException e) {
				log.warn("Error during logout of user from issuer: " + issuerUri + ". Error: " + e.getMessage());
			}
			return;
		}

		var idTokenValue = idToken.getTokenValue();

		var redirectUri = generateAppUri(request);

		var logoutUrl = createKeycloakLogoutUrl(issuerUri, idTokenValue, redirectUri);

		try {
			response.sendRedirect(logoutUrl);
		} catch (IOException e) {
			log.error("Could not send redirect to logoutUrl", e);
		}

	}

	private String generateAppUri(HttpServletRequest request) {
		var hostname = request.getServerName() + ":" + request.getServerPort();
		var isStandardHttps = "https".equals(request.getScheme()) && request.getServerPort() == 443;
		var isStandardHttp = "http".equals(request.getScheme()) && request.getServerPort() == 80;
		if (isStandardHttps || isStandardHttp) {
			hostname = request.getServerName();
		}
		return request.getScheme() + "://" + hostname + request.getContextPath() + "/";
	}

	private String createKeycloakLogoutUrl(String issuerUri, String idTokenValue, String redirectUri) {
		return issuerUri + "/protocol/openid-connect/logout?id_token_hint=" + idTokenValue + "&post_logout_redirect_uri=" + redirectUri;
	}
}
