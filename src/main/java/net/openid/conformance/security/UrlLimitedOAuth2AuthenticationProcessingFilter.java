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

		if (!request.getScheme().equals("https")) {
			// It's important that the reverse proxy settings are correct - if we receive a request that appears to be http here, then, e.g., we will send the user to the http version of the login page when they logout.
			throw new RuntimeException("A non-https request has been received by the conformance suite. The external interface should always use https; if https is in use then there may be a problem with the reverse-proxy apache in front of the suite not correctly setting the X-Forwarded-Port (etc) http headers correctly.");
		}

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
