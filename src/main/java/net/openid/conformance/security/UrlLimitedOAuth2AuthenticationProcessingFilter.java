package net.openid.conformance.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

// TODO FIXME rework this class
public class UrlLimitedOAuth2AuthenticationProcessingFilter
//	extends OAuth2AuthenticationProcessingFilter
	implements Filter {
//
//	private RequestMatcher matcher = AnyRequestMatcher.INSTANCE;
//
//	/* (non-Javadoc)
//	 * @see org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter#doFilter(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
//	 */
//	@Override
//	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
//
//		final HttpServletRequest request = (HttpServletRequest) req;
//
//		if (!request.getScheme().equals("https")) {
//			// It's important that the reverse proxy settings are correct - if we receive a request that appears to be http here, then, e.g., we will send the user to the http version of the login page when they logout.
//			throw new RuntimeException("A non-https request has been received by the conformance suite. The external interface should always use https; if https is in use then there may be a problem with the reverse-proxy apache in front of the suite not setting the X-Forwarded-Proto (etc) http headers correctly.");
//		}
//
//		if (matcher.matches(request)) {
//			super.doFilter(req, res, chain);
//		} else {
//			// skip this filter entirely
//			chain.doFilter(req, res);
//		}
//
//	}
//
//
//	/**
//	 * @return the matcher
//	 */
//	public RequestMatcher getMatcher() {
//		return matcher;
//	}
//
//
//	/**
//	 * @param matcher the matcher to set
//	 */
//	public void setMatcher(RequestMatcher matcher) {
//		this.matcher = matcher;
//	}


	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		filterChain.doFilter(servletRequest, servletResponse);
	}
}
