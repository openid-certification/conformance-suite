package net.openid.conformance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Brings a user back to the page they signed in from. The page's "Sign in" link carries the page
 * as {@code returnTo}, the login page passes it on to {@code /oauth2/authorization/{provider}},
 * {@link RememberFilter} keeps it in the session for the round trip to the identity provider, and
 * {@link SuccessHandler} redirects there once the login has completed. When the login fails,
 * {@link FailureHandler} hands the page back to the login page so that a retry returns to it too.
 *
 * <p>Only one of the suite's own pages is accepted: a path of the form {@code /<name>.html},
 * optionally with a query, so the parameter cannot be used to send a user anywhere else.
 */
public final class LoginReturnTo {

	public static final String PARAMETER = "returnTo";

	static final String SESSION_ATTRIBUTE = LoginReturnTo.class.getName();

	private static final String AUTHORIZATION_PATH = "/oauth2/authorization/";

	private static final int MAX_LENGTH = 2048;

	private static final Pattern SUITE_PAGE = Pattern.compile("^/[A-Za-z0-9_-]+\\.html(\\?[^\\s#\\\\]*)?$");

	private LoginReturnTo() {
	}

	/**
	 * @param target a page to return to, as given by the browser
	 * @return whether it is one of the suite's own pages
	 */
	static boolean isSuitePage(String target) {
		return target != null && target.length() <= MAX_LENGTH && SUITE_PAGE.matcher(target).matches();
	}

	/**
	 * When a login starts, keeps the page it should return to in the session, or forgets any page
	 * an earlier, abandoned login asked for.
	 */
	public static class RememberFilter extends OncePerRequestFilter {

		@Override
		protected boolean shouldNotFilter(HttpServletRequest request) {
			return !request.getRequestURI().startsWith(AUTHORIZATION_PATH);
		}

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
			String target = request.getParameter(PARAMETER);
			if (isSuitePage(target)) {
				request.getSession().setAttribute(SESSION_ATTRIBUTE, target);
			} else {
				HttpSession session = request.getSession(false);
				if (session != null) {
					session.removeAttribute(SESSION_ATTRIBUTE);
				}
			}
			filterChain.doFilter(request, response);
		}
	}

	/**
	 * Takes the page the current login was started from out of the session.
	 *
	 * @return the page, or null when the login was started without one
	 */
	private static String takePage(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		Object target = session.getAttribute(SESSION_ATTRIBUTE);
		session.removeAttribute(SESSION_ATTRIBUTE);
		return target instanceof String page && isSuitePage(page) ? page : null;
	}

	/**
	 * After a login, redirects to the page the login was started from, if there is one, and
	 * otherwise behaves as the default handler does.
	 */
	public static class SuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

		@Override
		public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws ServletException, IOException {
			String page = takePage(request);
			if (page != null) {
				clearAuthenticationAttributes(request);
				getRedirectStrategy().sendRedirect(request, response, page);
				return;
			}
			super.onAuthenticationSuccess(request, response, authentication);
		}
	}

	/**
	 * After a failed login, sends the user back to the login page with the error and, when the
	 * login was started from a page, with that page as {@code returnTo}, so that the retry the
	 * login page offers returns to it.
	 */
	public static class FailureHandler implements AuthenticationFailureHandler {

		@Override
		public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException {
			StringBuilder url = new StringBuilder("/login.html?error=")
				.append(URLEncoder.encode(String.valueOf(exception.getMessage()), StandardCharsets.UTF_8));
			String page = takePage(request);
			if (page != null) {
				url.append('&').append(PARAMETER).append('=').append(URLEncoder.encode(page, StandardCharsets.UTF_8));
			}
			response.sendRedirect(url.toString());
		}
	}
}
