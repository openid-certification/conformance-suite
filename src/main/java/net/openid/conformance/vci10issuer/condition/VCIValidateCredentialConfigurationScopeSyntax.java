package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.ScopeTokenSyntaxUtil;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that each {@code credential_configurations_supported.*.scope} value is a single
 * RFC 6749 §A.4 scope-token (visible ASCII, no SP / DQUOTE / BACKSLASH). OID4VCI defines the
 * scope as a single scope-token, so a space inside is also a violation.
 */
public class VCIValidateCredentialConfigurationScopeSyntax extends AbstractVciCredentialConfigurationsCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		List<String> issues = new ArrayList<>();
		forEachCredentialConfiguration(env, (configId, config) -> {
			JsonElement scopeEl = config.get("scope");
			if (scopeEl == null) {
				return;
			}
			if (!OIDFJSON.isString(scopeEl)) {
				issues.add(String.format("credential_configurations_supported.%s.scope: expected string, got %s",
					configId, scopeEl));
				return;
			}
			String scope = OIDFJSON.getString(scopeEl);
			if (!ScopeTokenSyntaxUtil.isValidScopeToken(scope)) {
				issues.add(String.format("credential_configurations_supported.%s.scope: '%s' is not a valid RFC 6749 §A.4 scope-token",
					configId, scope));
			}
		});

		if (!issues.isEmpty()) {
			throw error("Invalid scope syntax in credential_configurations_supported",
				args("issues", issues));
		}

		logSuccess("All credential_configurations_supported scopes are valid RFC 6749 scope-tokens");
		return env;
	}
}
