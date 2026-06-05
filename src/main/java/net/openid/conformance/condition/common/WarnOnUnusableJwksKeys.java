package net.openid.conformance.condition.common;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWKUtil.JwkIssue;

import java.util.List;

/**
 * Reports (as a warning, when the caller sets the result to WARNING) keys in {@code jwks_to_validate}
 * that the JOSE library cannot use: an unsupported key type, an unsupported curve, or an unrecognised
 * algorithm. Such keys are legitimate for a counterparty to publish - a recipient ignores keys it
 * cannot use (RFC 7517 section 5) - but are flagged so they remain visible in the test log rather
 * than being silently skipped.
 */
public class WarnOnUnusableJwksKeys extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jwks_to_validate", strings = "jwks_source_label")
	public Environment evaluate(Environment env) {

		String label = env.getString("jwks_source_label");
		JsonObject jwks = env.getObject("jwks_to_validate");

		List<JwkIssue> issues = JWKUtil.findUnusableKeys(jwks);
		if (!issues.isEmpty()) {
			JwkIssue first = issues.get(0);
			throw error("The JWK set in " + label + " contains " + issues.size()
					+ " key(s) that the test suite cannot use (e.g. unsupported key type, curve, or "
					+ "algorithm). The key at index " + first.index() + " " + first.detail() + ".",
				args("jwks_source", label, "issues", JWKUtil.issuesToJson(issues)));
		}

		logSuccess("All keys in the JWK set in " + label + " use a supported key type, curve and algorithm",
			args("jwks_source", label));
		return env;
	}
}
