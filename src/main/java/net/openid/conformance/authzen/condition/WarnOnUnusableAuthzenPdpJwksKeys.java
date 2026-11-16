package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.WarnOnUnusableJwksKeys;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWKUtil.JwkIssue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link WarnOnUnusableJwksKeys} for the PDP JWK Set. Section 9.1.3 lets signed_metadata be
 * "signed or MACed", and the suite verifies an HMAC-signed JWT with an "oct" key, but the shared
 * check reports an HMAC algorithm on such a key as unrecognized because the specifications it
 * serves elsewhere do not allow one. This reports the same unusable keys except an "oct" key
 * whose alg is an HMAC JWS algorithm. Intended to be called at WARNING.
 */
public class WarnOnUnusableAuthzenPdpJwksKeys extends AbstractCondition {

	private static final Set<String> HMAC_ALGORITHMS = JWSAlgorithm.Family.HMAC_SHA.stream()
		.map(JWSAlgorithm::getName)
		.collect(Collectors.toUnmodifiableSet());

	@Override
	@PreEnvironment(required = "jwks_to_validate", strings = "jwks_source_label")
	public Environment evaluate(Environment env) {
		String label = env.getString("jwks_source_label");
		JsonObject jwks = env.getObject("jwks_to_validate");

		List<JwkIssue> issues = JWKUtil.findUnusableKeys(jwks).stream()
			.filter(issue -> !isHmacKey(issue.key()))
			.toList();
		if (!issues.isEmpty()) {
			JwkIssue first = issues.get(0);
			throw error("The JWK set in " + label + " contains " + issues.size()
					+ " key(s) that the test suite cannot use (e.g. unsupported key type, curve, or "
					+ "algorithm). The key at index " + first.index() + " " + first.detail() + ".",
				args("jwks_source", label, "issues", JWKUtil.issuesToJson(issues)));
		}

		logSuccess("All keys in the JWK set in " + label + " use a supported key type, curve and algorithm, "
				+ "counting HMAC algorithms on \"oct\" keys, which can verify a MACed signed_metadata",
			args("jwks_source", label));
		return env;
	}

	/** True if {@code keyElement} is an "oct" key whose alg is an HMAC JWS algorithm. */
	static boolean isHmacKey(JsonElement keyElement) {
		if (keyElement == null || !keyElement.isJsonObject()) {
			return false;
		}
		JsonObject key = keyElement.getAsJsonObject();
		JsonElement kty = key.get("kty");
		JsonElement alg = key.get("alg");
		return isString(kty) && "oct".equals(OIDFJSON.getString(kty))
			&& isString(alg) && HMAC_ALGORITHMS.contains(OIDFJSON.getString(alg));
	}

	private static boolean isString(JsonElement element) {
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
	}
}
