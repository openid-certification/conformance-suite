package net.openid.conformance.condition.common;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWKUtil.JwkIssue;

import java.util.List;

/**
 * Fails if the JWK set in {@code jwks_to_validate} contains any private or symmetric key material.
 * Detection is a raw JSON member scan (see {@link JWKUtil#findPrivateOrSymmetricKeyMembers}), so a
 * private key the JOSE library cannot parse - e.g. on an unsupported curve, or with an unknown key
 * type - is still caught (the library would otherwise silently drop or fail to inspect it).
 */
public class EnsureJwksHasNoPrivateOrSymmetricKeyMaterial extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jwks_to_validate", strings = "jwks_source_label")
	public Environment evaluate(Environment env) {

		String label = env.getString("jwks_source_label");
		JsonObject jwks = env.getObject("jwks_to_validate");

		List<JwkIssue> issues = JWKUtil.findPrivateOrSymmetricKeyMembers(jwks);
		if (!issues.isEmpty()) {
			JwkIssue first = issues.get(0);
			throw error("The JWK set in " + label + " contains private or symmetric key material; "
					+ "a published JWK set must contain public keys only. The key at index "
					+ first.index() + " " + first.detail() + ".",
				args("jwks_source", label, "issues", JWKUtil.issuesToJson(issues)));
		}

		logSuccess("The JWK set in " + label + " contains only public key material",
			args("jwks_source", label));
		return env;
	}
}
