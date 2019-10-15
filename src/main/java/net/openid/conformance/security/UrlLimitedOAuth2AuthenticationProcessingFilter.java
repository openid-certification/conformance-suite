package net.openid.conformance.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class UrlLimitedOAuth2AuthenticationProcessingFilter extends OAuth2AuthenticationProcessingFilter {

	private RequestMatcher matcher = AnyRequestMatcher.INSTANCE;

	/* (non-Javadoc)
	 * @see org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		final HttpServletRequest request = (HttpServletRequest) req;

		if (matcher.matches(request)) {
			super.doFilter(req, res, chain);
		} else {
			// skip this filter entirely
			chain.doFilter(req, res);
		}

	}


	/**
	 * @return the matcher
	 */
	public RequestMatcher getMatcher() {
		return matcher;
	}


	/**
	 * @param matcher the matcher to set
	 */
	public void setMatcher(RequestMatcher matcher) {
		this.matcher = matcher;
	}

}
