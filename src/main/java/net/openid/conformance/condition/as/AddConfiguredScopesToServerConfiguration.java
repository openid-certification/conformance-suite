package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractConfiguredScopesCondition;
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
public class AddConfiguredScopesToServerConfiguration extends AbstractConfiguredScopesCondition {

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
}
