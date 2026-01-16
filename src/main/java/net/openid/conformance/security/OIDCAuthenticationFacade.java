package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class OIDCAuthenticationFacade implements AuthenticationFacade {

	public static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
	public static final SimpleGrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");

	// this gets set by the test runners and used later on
	@SuppressWarnings({"ThreadLocals", "ThreadLocalUsage"})
	private ThreadLocal<Authentication> localAuthentication = new ThreadLocal<>();

	@Override
	public void setLocalAuthentication(Authentication a) {
		localAuthentication.set(a);
	}

	@Override
	public Authentication getContextAuthentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	/**
	 * If the security context has an Authentication object, return it.
	 * <p>
	 * If not, return anything saved in the thread-local localAuthentication since
	 * we might be running in a background task.
	 *
	 * @return
	 */
	private Authentication getAuthentication() {
		Authentication a = getContextAuthentication();
		if (a != null) {
			return a;
		} else {
			return localAuthentication.get();
		}
	}

	private boolean hasAuthority(GrantedAuthority authority) {
		Authentication a = getAuthentication();
		if (a != null) {
			return a.getAuthorities().contains(authority);
		}
		return false;
	}

	/**
	 * Check to see if the current logged in user has the ROLE_ADMIN authority
	 */
	@Override
	public boolean isAdmin() {
		return hasAuthority(ROLE_ADMIN);
	}

	@Override
	public boolean isUser() {
		return hasAuthority(ROLE_USER);
	}

	@Override
	public ImmutableMap<String, String> getPrincipal() {
		Authentication a = getAuthentication();
		if (a == null) {
			return null;
		}
		String issuer = "";
		String subject = "";
		if (a instanceof OAuth2AuthenticationToken oidcToken) {
			var oidcUser = (OidcUser) oidcToken.getPrincipal();
			issuer = oidcUser.getIssuer().toString();
			subject = oidcUser.getSubject();
		} else if(a instanceof JwtAuthenticationToken jwtToken) {
			var jwt = (Jwt) jwtToken.getPrincipal();
			issuer = jwt.getIssuer().toString();
			subject = jwt.getSubject();
		} else {
			return null;
		}

		return ImmutableMap.of(
			"sub", subject,
			"iss", issuer
		);
	}

	@Override
	public String getDisplayName() {
		Authentication a = getAuthentication();
		if (a instanceof OAuth2AuthenticationToken oidcToken) {
			var oidcUser = (OidcUser) oidcToken.getPrincipal();
			if (oidcUser.getIdToken().getEmail() != null) {
				return oidcUser.getIdToken().getEmail();
			}

			return oidcUser.getIdToken().getFullName();
		} else if(a instanceof Jwt jwt) {
			if (jwt.getClaimAsString("mail") != null) {
				return jwt.getClaimAsString("mail");
			}

			return jwt.getClaimAsString("name");
		}
		return "";
	}
}
