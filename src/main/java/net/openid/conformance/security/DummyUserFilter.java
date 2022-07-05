package net.openid.conformance.security;

import java.io.IOException;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import com.google.common.collect.ImmutableSet;

/**
 * DummyUserFilter is used to inject an authenticated <code>OIDCAuthenticationToken</code>
 * into the Security Context. This should <b>NEVER</b> be used in production
 *
 * The point of this is to fake out the OIDC authentication mechanism of Spring into thinkin a user has already logged
 * in, so that a developer doesn't have to keep logging in every time they want to test a code change.
 *
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
			UserInfo info = new DefaultUserInfo();
			String email = makeDummyUserAdminInDevMode ? "DEVMODE@developer.com" : "DEVMODE_NO_ADMIN@developer.com";
			info.setEmail(email);
			info.setName("DEV MODE");
			info.setSub(sub);

			Set<GrantedAuthority> authorities = makeDummyUserAdminInDevMode
				? ImmutableSet.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
				: ImmutableSet.of(new SimpleGrantedAuthority("ROLE_USER"));

			SecurityContextHolder.getContext().setAuthentication(new OIDCAuthenticationToken(sub, issuer, info, authorities, null, null, null));
		}
		chain.doFilter(request, response);
	}
}
