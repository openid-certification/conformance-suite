package net.openid.conformance.condition.common;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWKUtil.JwkIssue;

import java.util.List;

/**
 * Fails if any <em>usable</em> key in {@code jwks_to_validate} cannot be parsed by the JOSE library.
 * This applies the checks the structural scan does not (e.g. the x5c bare-key-matches-certificate
 * check, and crypto-level validity). Keys the library cannot use (unknown key type, unsupported
 * curve, unrecognised algorithm) are skipped here - they are surfaced as a warning by
 * {@link WarnOnUnusableJwksKeys}, not failed.
 */
public class ParseUsableJwksKeys extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jwks_to_validate", strings = "jwks_source_label")
	public Environment evaluate(Environment env) {

		String label = env.getString("jwks_source_label");
		JsonObject jwks = env.getObject("jwks_to_validate");

		List<JwkIssue> issues = JWKUtil.findUnparseableUsableKeys(jwks);
		if (!issues.isEmpty()) {
			JwkIssue first = issues.get(0);
			throw error("The JWK set in " + label + " contains a key that should be usable but the JOSE "
					+ "library cannot parse. The key at index " + first.index() + " " + first.detail() + ".",
				args("jwks_source", label, "issues", JWKUtil.issuesToJson(issues)));
		}

		logSuccess("All usable keys in the JWK set in " + label + " parse successfully",
			args("jwks_source", label));
		return env;
	}
}
