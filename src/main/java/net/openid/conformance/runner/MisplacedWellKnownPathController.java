package net.openid.conformance.runner;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.util.OAuthUriUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * Answers requests for a well-known name appended to the host instead of inserted after
 * "/.well-known/" (for example {@code /oauth-authorization-server/test/a/alias} rather than
 * {@code /.well-known/oauth-authorization-server/test/a/alias}). Outside the permitted paths
 * the security chain redirects anonymous requests to the login page, which an implementer
 * reads as the metadata endpoint needing a login. This 404 says where the metadata is.
 */
@Controller
@Hidden
public class MisplacedWellKnownPathController {

	/**
	 * Matches the well-known names the suite serves under {@link TestDispatcher#WELL_KNOWN} when
	 * they appear as the first path segment instead, with or without a path after them.
	 */
	public static final String MISPLACED_PATH_PATTERN =
		"/{name:oauth-authorization-server|openid-configuration|openid-credential-issuer|openid-federation|ssf-configuration|authzen-configuration}/**";

	private final String baseUrl;

	public MisplacedWellKnownPathController(@Value("${fintechlabs.base_url}") String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@RequestMapping(value = MISPLACED_PATH_PATTERN, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> handle(HttpServletRequest request) {
		String path = request.getRequestURI();
		String expectedUrl = baseUrl + TestDispatcher.WELL_KNOWN + OAuthUriUtil.stripTrailingSlash(path.substring(1));
		return new ResponseEntity<>(Map.of(
			"error", "No metadata is served at " + path + ". The well-known path is formed by inserting "
				+ "'/.well-known/<name>' between the host and the path of the issuer URL, as per RFC 8414 section 3.1, "
				+ "so the URL to fetch is " + expectedUrl,
			"expected_url", expectedUrl),
			HttpStatus.NOT_FOUND);
	}
}
