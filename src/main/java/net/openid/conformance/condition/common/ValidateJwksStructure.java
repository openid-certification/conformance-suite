package net.openid.conformance.condition.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWKUtil.JwkIssue;

import java.util.List;

/**
 * Fails if the JWK set in {@code jwks_to_validate} is structurally invalid: not a JWK set object,
 * a key missing the members required for its key type, or a coordinate value that is not unpadded
 * base64url. Keys whose key type the JOSE library does not recognise are not failed here - they are
 * surfaced as a warning by {@link WarnOnUnusableJwksKeys}.
 */
public class ValidateJwksStructure extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jwks_to_validate", strings = "jwks_source_label")
	public Environment evaluate(Environment env) {

		String label = env.getString("jwks_source_label");
		JsonObject jwks = env.getObject("jwks_to_validate");

		JsonElement keys = jwks.get("keys");
		if (keys == null || !keys.isJsonArray()) {
			throw error("The JWK set in " + label + " does not contain a 'keys' array",
				args("jwks_source", label, "jwks", jwks));
		}

		List<JwkIssue> issues = JWKUtil.findStructurallyInvalidKeys(jwks);
		if (!issues.isEmpty()) {
			JwkIssue first = issues.get(0);
			throw error("The JWK set in " + label + " is structurally invalid. The key at index "
					+ first.index() + " " + first.detail() + ".",
				args("jwks_source", label, "issues", JWKUtil.issuesToJson(issues)));
		}

		logSuccess("The JWK set in " + label + " is structurally valid", args("jwks_source", label));
		return env;
	}
}
