package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractConfiguredScopesCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Checks that every scope the test is configured to request appears in the authorization
 * server's 'scopes_supported'.
 *
 * scopes_supported is only RECOMMENDED, and both OpenID Connect Discovery and RFC8414 permit a
 * server to not advertise scopes it supports, so callers should treat a failure as a warning.
 */
public class CheckDiscEndpointScopesSupportedContainsConfiguredScopes extends AbstractConfiguredScopesCondition {

	@Override
	@PreEnvironment(required = {"config", "server"})
	public Environment evaluate(Environment env) {

		JsonElement scopesSupported = env.getElementFromObject("server", "scopes_supported");
		if (scopesSupported == null || !scopesSupported.isJsonArray()) {
			throw error("'scopes_supported' in the server's discovery document is not an array",
				args("scopes_supported", scopesSupported));
		}
		List<String> supported = OIDFJSON.convertJsonArrayToList(scopesSupported.getAsJsonArray());

		Set<String> requested = configuredScopes(env);
		if (requested.isEmpty()) {
			logSuccess("No scope is set in the test configuration, so there is nothing to check");
			return env;
		}

		List<String> missing = new ArrayList<>();
		for (String scope : requested) {
			if (!supported.contains(scope)) {
				missing.add(scope);
			}
		}

		if (!missing.isEmpty()) {
			throw error("The 'scope' set in the test configuration contains scopes the server does not list " +
					"in the 'scopes_supported' of its discovery document. The server is permitted to not " +
					"advertise scopes it supports, but this may also mean the configured scope is incorrect.",
				args("missing", missing, "requested", requested, "scopes_supported", scopesSupported));
		}

		logSuccess("The server's 'scopes_supported' contains all the scopes set in the test configuration",
			args("requested", requested, "scopes_supported", scopesSupported));

		return env;
	}
}
