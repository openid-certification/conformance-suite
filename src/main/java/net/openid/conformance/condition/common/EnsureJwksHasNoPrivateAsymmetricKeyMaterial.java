package net.openid.conformance.condition.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.KeyType;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWKUtil.JwkIssue;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Fails if the JWK set in {@code jwks_to_validate} contains any private asymmetric key material.
 * Detection is a raw JSON member scan (see {@link JWKUtil#findPrivateOrSymmetricKeyMembers}), so a
 * private key the JOSE library cannot parse - e.g. on an unsupported curve, or with an unknown key
 * type - is still caught (the library would otherwise silently drop or fail to inspect it).
 * Symmetrical keys are ignored.
 */
public class EnsureJwksHasNoPrivateAsymmetricKeyMaterial extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jwks_to_validate", strings = "jwks_source_label")
	public Environment evaluate(Environment env) {

		String label = env.getString("jwks_source_label");
		JsonObject jwks = env.getObject("jwks_to_validate");

		List<JwkIssue> issues = JWKUtil.findPrivateOrSymmetricKeyMembers(jwks);
		if (!issues.isEmpty()) {
			// Filter out symmetric keys
			List<JwkIssue> asymmetricKeyIssues = issues.stream().filter(issue -> {
				if(issue.key().isJsonObject()) {
					JsonObject keyObj = issue.key().getAsJsonObject();
					return !KeyType.OCT.getValue().equals(stringMember(keyObj, "kty"));
				}
				return false;
			}).collect(Collectors.toUnmodifiableList());

			if(!asymmetricKeyIssues.isEmpty()) {
				JwkIssue first = asymmetricKeyIssues.get(0);
				throw error("The JWK set in " + label + " contains private asymmetric key material; "
						+ "a published JWK set must contain public keys only. The key at index "
						+ first.index() + " " + first.detail() + ".",
					args("jwks_source", label, "issues", JWKUtil.issuesToJson(issues)));
			}
		}

		logSuccess("The JWK set in " + label + " contains only public asymmetrical key material",
			args("jwks_source", label));
		return env;
	}

	private String stringMember(JsonObject key, String member) {
		JsonElement el = key.get(member);
		return (el != null && el.isJsonPrimitive()) ? OIDFJSON.getString(el) : null;
	}

}
