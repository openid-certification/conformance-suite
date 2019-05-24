package io.fintechlabs.testframework.security;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.web.filter.GenericFilterBean;

import io.fintechlabs.testframework.token.TokenService;

@Component
public class ApiTokenFilter extends GenericFilterBean {

	private static Pattern authPattern = Pattern.compile("^Basic (\\S+)$", Pattern.CASE_INSENSITIVE);

	@Autowired
	private TokenService tokenService;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		String token = getBearerToken(request);

		Authentication auth = null;
		if (token != null)
			auth = tokenService.getAuthenticationForToken(token);

		if (auth != null)
			SecurityContextHolder.getContext().setAuthentication(auth);

		chain.doFilter(request, response);
	}

	private static String getBearerToken(ServletRequest request) {

		if (!(request instanceof HttpServletRequest))
			return null;

		HttpServletRequest httpRequest = (HttpServletRequest) request;

		String authHeader = httpRequest.getHeader("Authorization");
		if (authHeader == null)
			return null;

		Matcher matcher = authPattern.matcher(authHeader);
		if (!matcher.matches())
			return null;

		String credsEnc = matcher.group(1);
		String[] credParts;
		try {
			byte[] credsRaw = Base64Utils.decodeFromString(credsEnc);
			credParts = new String(credsRaw).split(":");
		} catch (IllegalArgumentException e) {
			return null;
		}

		if (credParts.length != 2)
			return null;

		return credParts[1];
	}
}
