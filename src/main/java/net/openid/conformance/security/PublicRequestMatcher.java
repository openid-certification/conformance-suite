package net.openid.conformance.security;

import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Matches if the current request URI contains a {@code public} query parameter.
 */
public class PublicRequestMatcher implements RequestMatcher {

	@Override
	public boolean matches(HttpServletRequest request) {

		return Boolean.parseBoolean(request.getParameter("public"));
	}

}
