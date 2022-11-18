package net.openid.conformance.security;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
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
				? ImmutableSet.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
				: ImmutableSet.of(new SimpleGrantedAuthority("ROLE_USER"));
			String email = makeDummyUserAdminInDevMode ? "DEVMODE@developer.com" : "DEVMODE_NO_ADMIN@developer.com";
			Map<String, Object> atts = new HashMap<>();
			atts.put("email", email);
			atts.put("name", "DEV MODE");
			atts.put("sub", sub);
			atts.put("iss", issuer);
			OAuth2User info = new DefaultOAuth2User(authorities, atts, "sub");
			SecurityContextHolder.getContext().setAuthentication(new OAuth2AuthenticationToken(info, authorities, issuer));
		}
		chain.doFilter(request, response);
	}
}
