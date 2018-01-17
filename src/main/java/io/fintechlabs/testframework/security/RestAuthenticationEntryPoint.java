package io.fintechlabs.testframework.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class is to provide an alternate entry point into REST API request URLs.
 * If a request that is un-authenticated shows up rather than re-directing to the login page (as OIDC will do)
 * this just returns a 401 Unauthorized that JQuery, etc. can catch.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED,authException.getMessage());
	}
}
