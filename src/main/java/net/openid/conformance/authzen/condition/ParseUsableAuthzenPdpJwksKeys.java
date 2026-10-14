package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.ParseUsableJwksKeys;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWKUtil.JwkIssue;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@link ParseUsableJwksKeys} for the PDP JWK Set. The shared check skips a key whose alg it does
 * not recognize, which includes every HMAC algorithm, so an "oct" key that can verify a MACed
 * signed_metadata (Section 9.1.3) would never be parsed. This also parses each such key and
 * fails if it is shorter than the hash output of its alg, which RFC 7518 section 3.2 forbids.
 */
public class ParseUsableAuthzenPdpJwksKeys extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jwks_to_validate", strings = "jwks_source_label")
	public Environment evaluate(Environment env) {
		String label = env.getString("jwks_source_label");
		JsonObject jwks = env.getObject("jwks_to_validate");

		List<JwkIssue> issues = new ArrayList<>(JWKUtil.findUnparseableUsableKeys(jwks));
		if (JWKUtil.hasKeysArray(jwks)) {
			JsonArray keys = jwks.getAsJsonArray("keys");
			for (int i = 0; i < keys.size(); i++) {
				if (WarnOnUnusableAuthzenPdpJwksKeys.isHmacKey(keys.get(i))) {
					JwkIssue issue = checkHmacKey(i, keys.get(i).getAsJsonObject());
					if (issue != null) {
						issues.add(issue);
					}
				}
			}
		}
		if (!issues.isEmpty()) {
			issues.sort(Comparator.comparingInt(JwkIssue::index));
			JwkIssue first = issues.get(0);
			throw error("The JWK set in " + label + " contains a key that should be usable but the JOSE "
					+ "library cannot parse or use. The key at index " + first.index() + " " + first.detail() + ".",
				args("jwks_source", label, "issues", JWKUtil.issuesToJson(issues)));
		}

		logSuccess("All usable keys in the JWK set in " + label + " parse successfully, "
				+ "counting \"oct\" keys with an HMAC algorithm",
			args("jwks_source", label));
		return env;
	}

	private static JwkIssue checkHmacKey(int index, JsonObject key) {
		JWK jwk;
		try {
			jwk = JWK.parse(key.toString());
		} catch (ParseException e) {
			return new JwkIssue(index, key, "cannot be parsed by the JOSE library (" + e.getMessage() + ")");
		}
		String alg = OIDFJSON.getString(key.get("alg"));
		int minimumBits = Integer.parseInt(alg.substring(2));
		if (jwk.size() < minimumBits) {
			return new JwkIssue(index, key, "is " + jwk.size() + " bits long, shorter than the " + minimumBits
				+ " bits RFC 7518 section 3.2 requires for " + alg);
		}
		return null;
	}
}
