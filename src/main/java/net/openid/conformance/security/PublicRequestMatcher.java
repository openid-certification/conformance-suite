package net.openid.conformance.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.util.matcher.RequestMatcher;

public class PublicRequestMatcher implements RequestMatcher {

	@Override
	public boolean matches(HttpServletRequest request) {

		return Boolean.parseBoolean(request.getParameter("public"));
	}

}
