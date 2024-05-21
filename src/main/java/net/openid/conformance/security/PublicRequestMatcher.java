package net.openid.conformance.security;

import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;

public class PublicRequestMatcher implements RequestMatcher {

	@Override
	public boolean matches(HttpServletRequest request) {

		return Boolean.parseBoolean(request.getParameter("public"));
	}

}
