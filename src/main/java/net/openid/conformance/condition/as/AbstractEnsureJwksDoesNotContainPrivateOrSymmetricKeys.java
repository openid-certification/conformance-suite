package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWKUtil.JwkIssue;

import java.util.List;

public abstract class AbstractEnsureJwksDoesNotContainPrivateOrSymmetricKeys extends AbstractCondition {

	protected Environment verifyJwksDoesNotContainPrivateOrSymmetricKeys(Environment env, JsonObject jwks) {
		// Inspect the raw JSON members rather than parsing with the JOSE library: JWKSet.parse
		// silently drops a key with an unknown kty (RFC 7517 section 5) before isPrivate() could run,
		// so private material on such a key - or on a key using a curve the library cannot parse -
		// would otherwise go undetected. See JWKUtil.findPrivateOrSymmetricKeyMembers.
		List<JwkIssue> issues = JWKUtil.findPrivateOrSymmetricKeyMembers(jwks);
		if (issues.isEmpty()) {
			logSuccess("Jwks does not contain any private or symmetric keys");
			return env;
		}
		throw error("Jwks contains private and/or symmetric keys",
			args("keys", JWKUtil.issuesToJson(issues)));
	}

}
