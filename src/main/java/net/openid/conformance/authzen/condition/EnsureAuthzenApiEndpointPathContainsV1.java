package net.openid.conformance.authzen.condition;

import java.net.URI;
import java.net.URISyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Spec section 4 says Authzen API endpoint URIs SHOULD include the version
 * identifier `v1` in their path. This condition throws on absence so the
 * caller can surface it as a WARNING.
 */
public class EnsureAuthzenApiEndpointPathContainsV1 extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "authzen_api_endpoint")
	public Environment evaluate(Environment env) {
		String endpoint = env.getString("authzen_api_endpoint");
		URI uri;
		try {
			uri = new URI(endpoint);
		} catch (URISyntaxException e) {
			throw error("Authzen API endpoint URL is not a valid URI",
				args("endpoint", endpoint, "error", e.getMessage()));
		}
		String path = uri.getPath();
		if (path == null || !path.matches(".*/v1(/.*|$)")) {
			throw error("API endpoint path SHOULD include a `v1` version segment (spec section 4)",
				args("endpoint", endpoint, "path", path));
		}
		logSuccess("API endpoint path contains the expected `v1` version segment",
			args("endpoint", endpoint));
		return env;
	}
}
