package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adds the scopes the test's own clients are configured to request to the 'scopes_supported'
 * the discovery document publishes, so that the metadata this test publishes matches the
 * scopes the client under test will actually be asking for.
 *
 * Must run after any profile specific server configuration, as some profiles (Brazil) replace
 * scopes_supported wholesale.
 */
public class AddConfiguredScopesToServerConfiguration extends AbstractCondition {

	private static final String[] CLIENT_KEYS = { "client", "client2" };

	@Override
	@PreEnvironment(required = {"config", "server"})
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		Set<String> scopes = new LinkedHashSet<>();

		JsonObject server = env.getObject("server");
		JsonElement existing = server.get("scopes_supported");
		if (existing != null && !existing.isJsonNull()) {
			if (!existing.isJsonArray()) {
				throw error("'scopes_supported' in the server configuration is not an array", args("scopes_supported", existing));
			}
			scopes.addAll(OIDFJSON.convertJsonArrayToList(existing.getAsJsonArray()));
		}

		scopes.addAll(configuredScopes(env));

		if (scopes.isEmpty()) {
			// no scopes are configured and none were published; publishing an empty array would
			// be worse than omitting the (only RECOMMENDED) field entirely
			logSuccess("No scopes are configured, leaving 'scopes_supported' as it is", args("server", server));
			return env;
		}

		JsonArray scopesSupported = OIDFJSON.convertSetToJsonArray(scopes);
		server.add("scopes_supported", scopesSupported);

		logSuccess("Set 'scopes_supported' in the server metadata to include the configured scopes",
			args("scopes_supported", scopesSupported));

		return env;
	}

	/**
	 * The scopes are read from the test configuration rather than from the 'client' object in
	 * the environment, as the latter has profile specific per-transaction scopes (such as the
	 * Brazil 'consent:<consent id>' scope) added to it as the test runs; those are generated at
	 * runtime and no server could publish them.
	 *
	 * @return the union of the scopes configured for all the test's clients, in configuration
	 * order and without duplicates; empty if no client has a scope configured
	 */
	private Set<String> configuredScopes(Environment env) {
		Set<String> scopes = new LinkedHashSet<>();
		for (String clientKey : CLIENT_KEYS) {
			JsonElement scopeElement = env.getElementFromObject("config", clientKey + ".scope");
			if (scopeElement == null || scopeElement.isJsonNull()) {
				continue;
			}
			if (!scopeElement.isJsonPrimitive() || !scopeElement.getAsJsonPrimitive().isString()) {
				throw error("The 'scope' field in the client section of the test configuration is not a string",
					args("client", clientKey, "scope", scopeElement));
			}
			for (String scope : OIDFJSON.getString(scopeElement).split("\\s+")) {
				if (!scope.isEmpty()) {
					scopes.add(scope);
				}
			}
		}
		return scopes;
	}
}
