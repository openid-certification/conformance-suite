package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OIDCAuthenticationFacade implements AuthenticationFacade {

	public static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
	public static final SimpleGrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");

	// this gets set by the test runners and used later on
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

	private OAuth2AuthenticationToken getOAuth() {
		Authentication a = getAuthentication();
		if (a instanceof OAuth2AuthenticationToken) {
			return (OAuth2AuthenticationToken) a;
		}
		return null;
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
		OAuth2AuthenticationToken auth = getOAuth();
		if (auth == null) {
			return null;
		}

		OidcUser principal = (OidcUser) auth.getPrincipal();
		String issuer = principal.getIssuer().toString();
		String subject;
		String username;
		OidcUserInfo userInfo = principal.getUserInfo();
		if (userInfo != null) {
			subject = userInfo.getSubject();
			username = userInfo.getPreferredUsername() != null ? userInfo.getPreferredUsername() : principal.getName();
		} else {
			subject = principal.getSubject();
			username = principal.getName();
		}
		ImmutableMap<String, String> data = ImmutableMap.of(
			"iss", issuer,
			"sub", subject,
			"principal", username
		);

		return data;
	}

	@Override
	public String getDisplayName() {
		OAuth2AuthenticationToken auth = getOAuth();
		if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
			String displayName = oidcUser.getName();
			OidcUserInfo userInfo = oidcUser.getUserInfo();
			if (userInfo != null) {
				if (StringUtils.hasLength(userInfo.getEmail())) {
					displayName = userInfo.getEmail();
				} else if (StringUtils.hasLength((userInfo.getPreferredUsername()))) {
					displayName = userInfo.getPreferredUsername();
				} else if (StringUtils.hasLength((userInfo.getFullName()))) {
					displayName = userInfo.getFullName();
				}
				return displayName;
			}
			return displayName;
		} else if (auth != null) {
			return auth.getName();
		}
		return "";
	}

	@Override
	public OidcUserInfo getUserInfo() {
		OAuth2AuthenticationToken token = getOAuth();

		if (token == null || !(token.getPrincipal() instanceof OidcUser oidcUser)) {
			return null;
		}

		OidcUserInfo userInfo = oidcUser.getUserInfo();
		return userInfo;
	}
}
