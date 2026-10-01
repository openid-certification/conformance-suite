package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks that every scope the test is about to request appears in the authorization server's
 * 'scopes_supported'.
 *
 * The scopes are read from the 'client' and 'client2' objects in the environment rather than
 * from the test configuration, so that a profile that fixes the scope (ConnectID always
 * requests 'openid') is checked against what it really requests. The check must run before the
 * pre-authorization steps, as those add per-transaction scopes (such as the Brazil
 * 'consent:<consent id>' scope) that no server could publish.
 *
 * scopes_supported is only RECOMMENDED, and both OpenID Connect Discovery and RFC8414 permit a
 * server to not advertise scopes it supports, so callers should treat a failure as a warning.
 */
public class CheckDiscEndpointScopesSupportedContainsRequestedScopes extends AbstractCondition {

	private static final String[] CLIENT_KEYS = { "client", "client2" };

	@Override
	@PreEnvironment(required = {"client", "server"})
	public Environment evaluate(Environment env) {

		JsonElement scopesSupported = env.getElementFromObject("server", "scopes_supported");
		if (scopesSupported == null || !scopesSupported.isJsonArray()) {
			throw error("'scopes_supported' in the server's discovery document is not an array",
				args("scopes_supported", scopesSupported));
		}
		List<String> supported = OIDFJSON.convertJsonArrayToList(scopesSupported.getAsJsonArray());

		Set<String> requested = requestedScopes(env);
		if (requested.isEmpty()) {
			logSuccess("Neither client has a scope to request yet, so there is nothing to check");
			return env;
		}

		List<String> missing = new ArrayList<>();
		for (String scope : requested) {
			if (!supported.contains(scope)) {
				missing.add(scope);
			}
		}

		if (!missing.isEmpty()) {
			throw error("The scope the test will request contains scopes the server does not list in the " +
					"'scopes_supported' of its discovery document. The server is permitted to not advertise " +
					"scopes it supports, but this may also mean the 'scope' set in the test configuration " +
					"is incorrect.",
				args("missing", missing, "requested", requested, "scopes_supported", scopesSupported));
		}

		logSuccess("The server's 'scopes_supported' contains all the scopes the test will request",
			args("requested", requested, "scopes_supported", scopesSupported));

		return env;
	}

	/**
	 * @return the union of the scopes of all the test's clients, in client order and without
	 * duplicates; empty if no client has a scope yet
	 */
	private Set<String> requestedScopes(Environment env) {
		Set<String> scopes = new LinkedHashSet<>();
		for (String clientKey : CLIENT_KEYS) {
			JsonElement scopeElement = env.getElementFromObject(clientKey, "scope");
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
