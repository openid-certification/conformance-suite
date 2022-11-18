package net.openid.conformance.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OIDCAuthenticationFacade implements AuthenticationFacade {

	public static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
	public static final SimpleGrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");

	// used for the OAuth layer's issuer
	@Value("${oauth.introspection_url}")
	@SuppressWarnings("unused")
	private String introspectionUrl;

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
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, String> getPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication instanceof OAuth2AuthenticationToken) {
			OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
			Map attributes = oauth.getPrincipal().getAttributes();
			Map atts = new HashMap();
			atts.putAll(attributes);
			atts.put("iss", oauth.getAuthorizedClientRegistrationId());
			atts.put("sub", oauth.getPrincipal().getName());
			return atts;
		}

		if (authentication instanceof OAuth2LoginAuthenticationToken) {
			OAuth2LoginAuthenticationToken oauth = (OAuth2LoginAuthenticationToken) authentication;
			Map attributes = oauth.getPrincipal().getAttributes();
			Map atts = new HashMap();
			atts.putAll(attributes);
			atts.put("iss", oauth.getClientRegistration().getRegistrationId());
			atts.put("sub", oauth.getPrincipal().getName());
			return atts;
		}

		if (authentication instanceof OAuth2AuthorizationCodeAuthenticationToken) {
			OAuth2AuthorizationCodeAuthenticationToken oauth = (OAuth2AuthorizationCodeAuthenticationToken) authentication;
			Map attributes = oauth.getAdditionalParameters();
			Map atts = new HashMap();
			atts.putAll(attributes);
			atts.put("iss", oauth.getClientRegistration().getRegistrationId());
			atts.put("sub", oauth.getName());
			return atts;
		}

		return null;
	}

	@Override
	public String getDisplayName() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null) {
			OAuth2User usr = (OAuth2User) authentication.getPrincipal();
			return usr.getAttribute("email");
		}
		return "";
	}

	@Override
	public OAuth2User getUserInfo() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null) {
			return (OAuth2User) authentication.getPrincipal();
		} else {
			return null;
		}
	}
}
