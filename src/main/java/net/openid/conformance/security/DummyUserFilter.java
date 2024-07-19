package net.openid.conformance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DummyUserFilter is used to inject an authenticated <code>OIDCAuthenticationToken</code>
 * into the Security Context. This should <b>NEVER</b> be used in production
 * <p>
 * The point of this is to fake out the OIDC authentication mechanism of Spring into thinkin a user has already logged
 * in, so that a developer doesn't have to keep logging in every time they want to test a code change.
 * <p>
 * To enable this, add the filter into the <code>HttpSecurity</code> object in your
 * <code>WebSecurityConfigurerAdapter</code> with:
 *
 * <code>http.addFilterBefore(dummyUserFilter(), OIDCAuthenticationFilter.class);</code>
 */
@Component
public class DummyUserFilter extends GenericFilterBean {

	@Value("${fintechlabs.devmode:false}")
	private boolean devmode;

	@Value("${fintechlabs.makeDummyUserAdminInDevMode:true}")
	private boolean makeDummyUserAdminInDevMode;

	private static String sub = "developer";
	private static String issuer = "https://developer.com";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (devmode) {

			Set<GrantedAuthority> authorities = makeDummyUserAdminInDevMode
				? Set.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
				: Set.of(new SimpleGrantedAuthority("ROLE_USER"));

			Map<String, Object> claims = new HashMap<>();
			String email = makeDummyUserAdminInDevMode ? "DEVMODE@developer.com" : "DEVMODE_NO_ADMIN@developer.com";
			claims.put("email", email);
			claims.put("name", "DEV MODE");
			claims.put("given_name", "DEV");
			claims.put("family_name", "MODE");
			claims.put("sub", sub);
			claims.put("iss", issuer);

			OidcIdToken dummyIdToken = new OidcIdToken("dummy", Instant.now(), null, claims);

			OidcUserInfo dummyUserInfo = new OidcUserInfo(claims);

			DefaultOidcUser user = new DefaultOidcUser(authorities, dummyIdToken, dummyUserInfo, "sub");

			OAuth2AuthenticationToken oauth2AuthToken = new OAuth2AuthenticationToken(user, authorities, "dummy");
			SecurityContextHolder.getContext().setAuthentication(oauth2AuthToken);
		}
		chain.doFilter(request, response);
	}
}
