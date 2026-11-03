package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import net.openid.conformance.sharing.privatelink.PrivateLinkOneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthentication;
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
		String issuer;
		String subject;
		if (a instanceof OAuth2AuthenticationToken oauth) {
			if (!(oauth.getPrincipal() instanceof OidcUser oidcUser)) {
				throw unidentifiablePrincipal(a, "an OAuth2 login that is not an OIDC login");
			}
			issuer = oidcUser.getIssuer() == null ? null : oidcUser.getIssuer().toString();
			subject = oidcUser.getSubject();
		} else if (a instanceof JwtAuthenticationToken jwtToken) {
			Jwt jwt = jwtToken.getToken();
			issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
			subject = jwt.getSubject();
		} else if (a instanceof OneTimeTokenAuthentication ott) {
			PrivateLinkOneTimeToken privateToken = (PrivateLinkOneTimeToken) ott.getDetails();
			return ImmutableMap.copyOf(privateToken.getSharedAsset().getOwner());
		} else {
			// No recognised user is signed in (a back-channel call, or no
			// authentication at all).
			return null;
		}

		if (issuer == null || subject == null) {
			throw unidentifiablePrincipal(a, "a principal without an issuer and subject");
		}
		return ImmutableMap.of(
			"sub", subject,
			"iss", issuer
		);
	}

	/**
	 * Callers read a null principal as "nobody is signed in" and some of them then
	 * skip the owner check altogether (InMemoryTestRunnerSupport, DBTestPlanService),
	 * so a signed-in user who cannot be identified must fail the request instead.
	 */
	private static IllegalStateException unidentifiablePrincipal(Authentication a, String what) {
		return new IllegalStateException("Cannot determine the owner of " + what
			+ " (" + a.getClass().getSimpleName() + ")");
	}

	@Override
	public String getDisplayName() {
		Authentication a = getAuthentication();
		if (a instanceof OAuth2AuthenticationToken oauth) {
			if (oauth.getPrincipal() instanceof OidcUser oidcUser) {
				if (oidcUser.getEmail() != null) {
					return oidcUser.getEmail();
				}
				return oidcUser.getName();
			}
			return oauth.getName();
		} else if (a instanceof JwtAuthenticationToken jwtToken) {
			// An IdP access token may carry the user's email or name; an API token
			// carries only its owner's iss and sub, so it falls back to the subject.
			Jwt jwt = jwtToken.getToken();
			for (String claim : new String[] {"email", "name"}) {
				String value = jwt.getClaimAsString(claim);
				if (value != null) {
					return value;
				}
			}
			return jwt.getSubject() == null ? "" : jwt.getSubject();
		} else if (a instanceof OneTimeTokenAuthentication ott) {
			PrivateLinkOneTimeToken privateToken = (PrivateLinkOneTimeToken) ott.getDetails();
			return privateToken.getUsername();
		}
		return "";
	}
}
