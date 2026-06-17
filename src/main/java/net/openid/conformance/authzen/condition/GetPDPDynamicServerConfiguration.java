package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
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

public class GetPDPDynamicServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "pdp" } )
	public Environment evaluate(Environment env) {
		if (!env.containsObject("config")) {
			throw error("Couldn't find a configuration");
		}
		String pdpDecisionPoint = env.getString("config", "pdp.policy_decision_point");
		if (Strings.isNullOrEmpty(pdpDecisionPoint)) {
			throw error("'Policy Decision Point Identifier' field is missing from the 'PDP' section in the test configuration (required to derive the discovery URL when using dynamic server configuration)", args("config", env.getObject("config")));
		}
		String discoveryUrl = deriveDiscoveryUrl(pdpDecisionPoint);

		// fetch the value
		String jsonString;
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			ResponseEntity<String> response = restTemplate.exchange(discoveryUrl, HttpMethod.GET, null, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("discovery", response);

			env.putObject("discovery_endpoint_response", responseInfo);

			jsonString = response.getBody();
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to fetch server configuration from " + discoveryUrl;
			if (e.getCause() != null) {
				msg += " - " +e.getCause().getMessage();
			}
			throw error(msg, e);
		}

		if (!Strings.isNullOrEmpty(jsonString)) {
			try {
				JsonObject serverConfig = JsonParser.parseString(jsonString).getAsJsonObject();

				logSuccess("Successfully parsed server configuration", serverConfig);

				env.putObject("pdp", serverConfig);

				return env;
			} catch (JsonSyntaxException e) {
				throw error(e, args("json", jsonString));
			}

		} else {
			throw error("empty server configuration");
		}

	}

	/**
	 * Derive the AuthZEN metadata well-known URL from the PDP base URL.
	 *
	 * <p>Per the certification profile (https://github.com/openid/authzen/issues/433
	 * §6.1, consistent with AuthZEN 1.0 and RFC 8414), the
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
			throw error("'Policy Decision Point Identifier' field in the test configuration is not a valid URL", e, args("policy_decision_point", pdpDecisionPoint));
		}
		if (uri.getScheme() == null || uri.getRawAuthority() == null) {
			throw error("'Policy Decision Point Identifier' field in the test configuration must be an absolute URL with a scheme and host", args("policy_decision_point", pdpDecisionPoint));
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
