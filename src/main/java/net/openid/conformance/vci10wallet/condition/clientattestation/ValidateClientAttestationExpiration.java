package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

/**
 * Validates that the client attestation JWT's 'exp' claim is present, not expired (with
 * clock skew tolerance), and not unreasonably far in the future.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.1">OAuth 2.0 Attestation-Based Client Authentication §5.1</a>
 */
public class ValidateClientAttestationExpiration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_attestation_object")
	public Environment evaluate(Environment env) {

		JsonElement expEl = env.getElementFromObject("client_attestation_object", "claims.exp");

		if (expEl == null) {
			throw error("Couldn't find exp claim in the client_attestation");
		}

		long exp = OIDFJSON.getLong(expEl);

		try {
			JWTUtil.validateExpClaim(exp);
		} catch (IllegalArgumentException e) {
			throw error("Client attestation JWT exp claim is invalid: " + e.getMessage(), args("exp", exp));
		}

		logSuccess("Client attestation JWT exp claim is valid", args("exp", exp));

		return env;
	}
}
