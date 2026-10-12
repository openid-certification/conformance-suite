package net.openid.conformance.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Set;

/**
 * Matches the pages the suite serves to a browser: the home page and the HTML files in the
 * page directory on the classpath. These are the only paths an anonymous user is redirected
 * to login for, and the only paths saved for replay after login.
 */
public class ServedPageRequestMatcher implements RequestMatcher {

	private static final Set<String> METHODS = Set.of("GET", "HEAD");

	private final String pageDirectory;

	/**
	 * @param pageDirectory classpath directory holding the pages, {@code static} or {@code static-legacy}
	 */
	public ServedPageRequestMatcher(String pageDirectory) {
		this.pageDirectory = pageDirectory;
	}

	@Override
	public boolean matches(HttpServletRequest request) {
		if (!METHODS.contains(request.getMethod())) {
			return false;
		}
		String path = request.getRequestURI();
		if (path.equals("/") || path.equals("/index.html")) {
			return true;
		}
		// one segment, no traversal: "/name.html"
		if (!path.startsWith("/") || !path.endsWith(".html") || path.indexOf('/', 1) >= 0) {
			return false;
		}
		return new ClassPathResource(pageDirectory + path).exists();
	}
}
