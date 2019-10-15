package net.openid.conformance.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.google.gson.JsonObject;

/**
 * This class is to provide an alternate entry point into REST API request URLs.
 * If a request that is un-authenticated shows up rather than re-directing to the login page (as OIDC will do)
 * this just returns a 401 Unauthorized that JQuery, etc. can catch.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setHeader("Content-Type", "application/json");
		JsonObject obj = new JsonObject();
		obj.addProperty("error", "Unauthorized");
		obj.addProperty("message", authException.getMessage());
		response.getOutputStream().print(obj.toString());
		response.flushBuffer();
	}
}
