package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.ScopeTokenSyntaxUtil;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that each entry in the authorization server metadata's {@code scopes_supported}
 * is a valid RFC 6749 Appendix A.4 scope-token (visible ASCII, no SP / DQUOTE / BACKSLASH).
 * {@code scopes_supported} is OPTIONAL (RFC 8414 §2 / OpenID Connect Discovery §3); its
 * absence is not an error.
 */
public class CheckDiscEndpointScopesSupportedSyntax extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement scopesSupportedEl = env.getElementFromObject("server", "scopes_supported");
		if (scopesSupportedEl == null) {
			logSuccess("authorization server metadata has no scopes_supported (OPTIONAL); nothing to validate");
			return env;
		}
		if (!scopesSupportedEl.isJsonArray()) {
			throw error("scopes_supported is not a JSON array", args("scopes_supported", scopesSupportedEl));
		}

		JsonArray scopesSupported = scopesSupportedEl.getAsJsonArray();
		List<String> issues = new ArrayList<>();
		for (int i = 0; i < scopesSupported.size(); i++) {
			JsonElement element = scopesSupported.get(i);
			if (!OIDFJSON.isString(element)) {
				issues.add(String.format("scopes_supported[%d]: expected string, got %s", i, element));
				continue;
			}
			String scope = OIDFJSON.getString(element);
			String syntaxError = ScopeTokenSyntaxUtil.scopeTokenSyntaxError(scope);
			if (syntaxError != null) {
				issues.add(String.format("scopes_supported[%d]: '%s' %s", i, scope, syntaxError));
			}
		}

		if (!issues.isEmpty()) {
			throw error("Invalid scope syntax in authorization server scopes_supported",
				args("issues", issues));
		}

		logSuccess("All scopes_supported entries are valid RFC 6749 scope-tokens");
		return env;
	}
}
