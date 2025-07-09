package net.openid.conformance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ensures that only https traffic is passed through the filter chain.
 */
public class RejectPlainHttpTrafficFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		if (!"https".equals(request.getScheme())) {
			// It's important that the reverse proxy settings are correct - if we receive a request that appears to be http here, then, e.g., we will send the user to the http version of the login page when they logout.
			throw new RuntimeException("A non-https request has been received by the conformance suite. The external interface should always use https; if https is in use then there may be a problem with the reverse-proxy apache in front of the suite not setting the X-Forwarded-Proto (etc) http headers correctly. URL: "+
				request.getRequestURI()+"    "+request.getRequestURL().toString());
		}
		filterChain.doFilter(request, response);
	}
}
