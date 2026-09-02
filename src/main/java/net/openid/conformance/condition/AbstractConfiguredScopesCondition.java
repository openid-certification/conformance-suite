package net.openid.conformance.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base for conditions that work from the scopes the test is configured to request, i.e. the
 * 'scope' fields in the 'Client' and 'Second client' sections of the test configuration.
 *
 * The scopes are read from the test configuration rather than from the 'client' object in the
 * environment, as the latter has profile specific per-transaction scopes (such as the Brazil
 * 'consent:<consent id>' scope) added to it as the test runs; those are generated at runtime
 * and no server could publish them.
 */
public abstract class AbstractConfiguredScopesCondition extends AbstractCondition {

	private static final String[] CLIENT_KEYS = { "client", "client2" };

	/**
	 * @return the union of the scopes configured for all the test's clients, in configuration
	 * order and without duplicates; empty if no client has a scope configured
	 */
	protected Set<String> configuredScopes(Environment env) {
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
