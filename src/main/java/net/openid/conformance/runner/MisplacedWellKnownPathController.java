package net.openid.conformance.runner;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.util.OAuthUriUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.Set;

/**
 * Answers requests for a well-known name used as the first path segment (for example
 * {@code /oauth-authorization-server/test/a/alias} rather than
 * {@code /.well-known/oauth-authorization-server/test/a/alias}). Outside the permitted paths
 * the security chain redirects anonymous requests to the login page, which an implementer
 * reads as the metadata endpoint needing a login. This 404 says where the metadata is.
 *
 * <p>The suite serves two layouts. OAuth authorization server metadata, credential issuer
 * metadata and SSF configuration follow RFC 8414 section 3.1: {@code /.well-known/<name>} is
 * inserted between the host and the issuer path. OpenID Provider configuration and the
 * Federation entity configuration are appended to the issuer or entity identifier instead
 * (OpenID Connect Discovery 1.0 section 4.1, OpenID Federation 1.0 section 9).
 */
@Controller
@Hidden
public class MisplacedWellKnownPathController {

	/**
	 * Matches the well-known names the suite serves when they appear as the first path segment
	 * instead, with or without a path after them.
	 */
	public static final String MISPLACED_PATH_PATTERN =
		"/{name:oauth-authorization-server|openid-configuration|openid-credential-issuer|openid-federation|ssf-configuration}/**";

	/** Names served at the path formed by inserting {@code /.well-known/<name>} after the host. */
	private static final Set<String> INSERTED_AFTER_HOST =
		Set.of("oauth-authorization-server", "openid-credential-issuer", "ssf-configuration");

	/** Names served at the path formed by appending {@code /.well-known/<name>}, with the defining spec. */
	private static final Map<String, String> APPENDED_TO_PATH = Map.of(
		"openid-configuration", "OpenID Connect Discovery 1.0 section 4.1",
		"openid-federation", "OpenID Federation 1.0 section 9");

	private final String baseUrl;

	public MisplacedWellKnownPathController(@Value("${fintechlabs.base_url}") String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * The response is JSON whatever the client's Accept header says: a {@code produces} condition
	 * would answer a client that accepts only, say, text/html or application/jwt with an empty
	 * 406, and the hint is what the client needs to see.
	 */
	@RequestMapping(MISPLACED_PATH_PATTERN)
	public ResponseEntity<Map<String, String>> handle(@PathVariable String name, HttpServletRequest request) {
		String path = request.getRequestURI();
		// the issuer path the client meant, without the misplaced name and any trailing slash
		String issuerPath = OAuthUriUtil.stripTrailingSlash(path.substring(name.length() + 1));
		String wellKnown = TestDispatcher.WELL_KNOWN + name;

		String expectedUrl;
		String rule;
		if (INSERTED_AFTER_HOST.contains(name)) {
			expectedUrl = baseUrl + wellKnown + issuerPath;
			rule = "inserting '" + wellKnown + "' between the host and the path of the issuer URL, as per RFC 8414 section 3.1";
		} else {
			expectedUrl = baseUrl + issuerPath + wellKnown;
			rule = "appending '" + wellKnown + "' to the issuer URL with any trailing slash removed, as per "
				+ APPENDED_TO_PATH.get(name);
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(Map.of(
			"error", "No metadata is served at " + path + ". The well-known path is formed by " + rule
				+ ", so the URL to fetch is " + expectedUrl,
			"expected_url", expectedUrl));
	}
}
