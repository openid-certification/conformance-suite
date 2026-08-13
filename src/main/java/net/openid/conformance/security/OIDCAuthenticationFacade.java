package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import net.openid.conformance.sharing.privatelink.PrivateLinkOneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class OIDCAuthenticationFacade implements AuthenticationFacade {

	public static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
	public static final SimpleGrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");
	public static final SimpleGrantedAuthority ROLE_PRIVATE_LINK_USER = new SimpleGrantedAuthority("ROLE_PRIVATE_LINK_USER");

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
	public boolean isPrivateLinkUser() {
		return hasAuthority(ROLE_PRIVATE_LINK_USER);
	}

	@Override
	public PrivateLinkOneTimeToken getPrivateOneTimeToken() {
		Authentication auth = getAuthentication();
		if (auth instanceof OneTimeTokenAuthentication ott) {
			PrivateLinkOneTimeToken privateToken = (PrivateLinkOneTimeToken) ott.getDetails();
			return privateToken;
		}
		return null;
	}

	@Override
	public ImmutableMap<String, String> getPrincipal() {
		Authentication a = getAuthentication();
		if (a == null) {
			return null;
		}
		String issuer = "";
		String subject = "";
		// Type test rather than a cast: an OAuth2AuthenticationToken from a plain
		// OAuth2 (non-OIDC) flow carries an OAuth2User, and a ClassCastException
		// here would break every request made by that principal.
		if (a instanceof OAuth2AuthenticationToken oidcToken && oidcToken.getPrincipal() instanceof OidcUser oidcUser) {
			if (oidcUser.getIssuer() == null) {
				return null;
			}
			issuer = oidcUser.getIssuer().toString();
			subject = oidcUser.getSubject();
		} else if(a instanceof JwtAuthenticationToken jwtToken) {
			var jwt = (Jwt) jwtToken.getPrincipal();
			if (jwt.getIssuer() == null) {
				return null;
			}
			issuer = jwt.getIssuer().toString();
			subject = jwt.getSubject();
		} else if (a instanceof OneTimeTokenAuthentication ott) {
			PrivateLinkOneTimeToken privateToken = (PrivateLinkOneTimeToken) ott.getDetails();
			return ImmutableMap.copyOf(privateToken.getSharedAsset().getOwner());
		} else {
			return null;
		}

		return ImmutableMap.of(
			"sub", subject,
			"iss", issuer
		);
	}

	private static String firstNonNull(String preferred, String fallback) {
		return preferred != null ? preferred : (fallback == null ? "" : fallback);
	}

	@Override
	public String getDisplayName() {
		Authentication a = getAuthentication();
		// Same type test as getPrincipal(): the principal of an OAuth2 (non-OIDC)
		// login is not an OidcUser, and casting it would fail the whole request.
		if (a instanceof OAuth2AuthenticationToken oidcToken && oidcToken.getPrincipal() instanceof OidcUser oidcUser) {
			OidcIdToken idToken = oidcUser.getIdToken();
			if (idToken == null) {
				return "";
			}
			if (idToken.getEmail() != null) {
				return idToken.getEmail();
			}
			// Falls back to the subject, which is what OidcUser#getName resolves to:
			// a principal with no human-readable claim still needs a stable label.
			return firstNonNull(idToken.getFullName(), oidcUser.getSubject());
		} else if (a instanceof JwtAuthenticationToken jwtToken) {
			// Jwt is a principal, not an Authentication, so this must match on the
			// token type - matching on Jwt itself is unreachable.
			var jwt = (Jwt) jwtToken.getPrincipal();
			for (String claim : new String[] {"email", "mail", "name"}) {
				String value = jwt.getClaimAsString(claim);
				if (value != null) {
					return value;
				}
			}
			// API tokens carry none of those claims, so without this an API-token
			// caller would get an empty displayName from /api/currentuser - the
			// pre-IdP code fell back to the subject here too.
			return firstNonNull(jwt.getSubject(), "");
		} else if (a instanceof OneTimeTokenAuthentication ott) {
			PrivateLinkOneTimeToken privateToken = (PrivateLinkOneTimeToken) ott.getDetails();
			return privateToken.getUsername();
		}
		return "";
	}
}
