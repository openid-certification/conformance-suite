package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

/**
 * Fetches the PDP metadata from the well-known URL derived from the 'PDP Identifier' (Section
 * 9.2.1) into the {@code pdp} environment object.
 *
 * <p>Constructed with {@code defaultPathsWhenNotFound}, an HTTP 404 is not a failure: the PDP
 * publishes no metadata, so Section 10.1 forms each endpoint URL from the default path, and
 * certification scenario 9.6 has the harness do so for every level but Discovery. The {@code pdp}
 * object then holds only the policy_decision_point, from which {@link AbstractSetAuthzenApiEndpoint}
 * builds the default paths, and {@code pdp_metadata_not_found} is set.
 */
public class GetPDPDynamicServerConfiguration extends AbstractCondition {

	public static final String METADATA_NOT_FOUND = "pdp_metadata_not_found";

	private final boolean defaultPathsWhenNotFound;

	public GetPDPDynamicServerConfiguration() {
		this(false);
	}

	public GetPDPDynamicServerConfiguration(boolean defaultPathsWhenNotFound) {
		this.defaultPathsWhenNotFound = defaultPathsWhenNotFound;
	}

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "pdp" } )
	public Environment evaluate(Environment env) {
		if (!env.containsObject("config")) {
			throw error("Couldn't find a configuration");
		}
		String pdpDecisionPoint = env.getString("config", "pdp.policy_decision_point");
		if (Strings.isNullOrEmpty(pdpDecisionPoint)) {
			throw error("'PDP Identifier' field is missing from the 'AuthZEN' section in the test configuration (required to derive the metadata URL when 'PDP server metadata location' is discovery)", args("config", env.getObject("config")));
		}
		String discoveryUrl = deriveDiscoveryUrl(pdpDecisionPoint);

		// fetch the value
		String jsonString;
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			ResponseEntity<String> response = restTemplate.exchange(discoveryUrl, HttpMethod.GET, null, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("discovery", response);

			env.putObject("discovery_endpoint_response", responseInfo);

			// The client follows no redirects and Spring throws only on 4xx and 5xx, so a 3xx arrives
			// here, as does a 2xx without a body. Neither carries metadata to parse, and the parse
			// error would not name the status. A 2xx other than 200 with a body is left to
			// EnsureDiscoveryMetadataResponseValid.
			int status = response.getStatusCode().value();
			if (response.getStatusCode().is3xxRedirection()) {
				throw error("Unable to fetch server configuration from " + discoveryUrl + ": the PDP answered HTTP " + status
					+ ", a redirect, which the suite does not follow. The metadata must be served at the well-known URL "
					+ "derived from the 'PDP Identifier' in the test configuration.",
					args("status", status, "location", response.getHeaders().getFirst("Location")));
			}
			jsonString = response.getBody();
			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Unable to fetch server configuration from " + discoveryUrl + ": the PDP answered HTTP " + status
					+ " with an empty body", args("status", status));
			}
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientResponseException e) {
			if (defaultPathsWhenNotFound && e.getStatusCode().value() == 404) {
				JsonObject pdp = new JsonObject();
				pdp.addProperty("policy_decision_point", pdpDecisionPoint);
				env.putObject("pdp", pdp);
				env.putBoolean(METADATA_NOT_FOUND, true);
				logSuccess("The PDP has no metadata at " + discoveryUrl + " (HTTP 404), so the test calls the API endpoints at their "
					+ "default paths under the 'PDP Identifier'. To call other endpoints, select 'static' for 'PDP server metadata "
					+ "location' and enter them in the test configuration.",
					args("status", 404, "policy_decision_point", pdpDecisionPoint));
				return env;
			}
			// The PDP answered with an error status; the exception has no cause to report.
			throw error("Unable to fetch server configuration from " + discoveryUrl + ": the PDP answered HTTP " + e.getStatusCode().value(),
				e, args("status", e.getStatusCode().value(), "body", e.getResponseBodyAsString()));
		} catch (RestClientException e) {
			String msg = "Unable to fetch server configuration from " + discoveryUrl;
			if (e.getCause() != null) {
				msg += " - " +e.getCause().getMessage();
			}
			throw error(msg, e);
		}

		JsonObject serverConfig = parseServerConfiguration(jsonString);
		logSuccess("Successfully parsed server configuration", serverConfig);
		env.putObject("pdp", serverConfig);
		return env;
	}

	/**
	 * Section 9.2.2: the metadata response body "MUST be a JSON object".
	 */
	JsonObject parseServerConfiguration(String jsonString) {
		if (Strings.isNullOrEmpty(jsonString)) {
			throw error("empty server configuration");
		}
		JsonElement parsed;
		try {
			parsed = JsonParser.parseString(jsonString);
		} catch (JsonSyntaxException e) {
			throw error("The PDP metadata response body is not valid JSON", e, args("json", jsonString));
		}
		if (!parsed.isJsonObject()) {
			throw error("The PDP metadata response body is not a JSON object", args("json", jsonString));
		}
		return parsed.getAsJsonObject();
	}

	/**
	 * Derive the AuthZEN metadata well-known URL from the PDP base URL.
	 *
	 * <p>Per Section 9.2 and certification scenario 9.1 (consistent with RFC 8414), the
	 * {@code /.well-known/authzen-configuration} segment is inserted <em>between
	 * the host and any existing path</em>, not appended after the whole URL.
	 * For example {@code https://pdp.example.com/tenant1} yields
	 * {@code https://pdp.example.com/.well-known/authzen-configuration/tenant1},
	 * and a base URL with no path yields
	 * {@code https://pdp.example.com/.well-known/authzen-configuration}.
	 */
	String deriveDiscoveryUrl(String pdpDecisionPoint) {
		final String wellKnown = "/.well-known/authzen-configuration";
		URI uri;
		try {
			uri = new URI(pdpDecisionPoint);
		} catch (URISyntaxException e) {
			throw error("'PDP Identifier' field in the test configuration is not a valid URL", e, args("policy_decision_point", pdpDecisionPoint));
		}
		if (uri.getScheme() == null || uri.getRawAuthority() == null) {
			throw error("'PDP Identifier' field in the test configuration must be an absolute URL with a scheme and host", args("policy_decision_point", pdpDecisionPoint));
		}
		String path = uri.getRawPath();
		String newPath;
		if (Strings.isNullOrEmpty(path) || "/".equals(path)) {
			newPath = wellKnown;
		} else {
			// Strip any trailing slash from the existing path so we don't produce a
			// double slash when re-joining (e.g. "/tenant1/" -> "/tenant1").
			String existingPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
			newPath = wellKnown + existingPath;
		}
		return uri.getScheme() + "://" + uri.getRawAuthority() + newPath;
	}

}
