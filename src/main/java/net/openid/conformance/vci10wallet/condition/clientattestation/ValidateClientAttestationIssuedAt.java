package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

/**
 * Validates the client attestation JWT's 'iat' claim if present. Per OAuth 2.0
 * Attestation-Based Client Authentication §5.1, 'iat' is OPTIONAL — the condition skips
 * when it is absent. When present, 'iat' must be a reasonable unix timestamp and must
 * not be in the future (allowing clock skew).
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.1">OAuth 2.0 Attestation-Based Client Authentication §5.1</a>
 */
public class ValidateClientAttestationIssuedAt extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_attestation_object")
	public Environment evaluate(Environment env) {

		JsonElement iatEl = env.getElementFromObject("client_attestation_object", "claims.iat");

		if (iatEl == null) {
			log("Client attestation JWT has no iat claim (optional), skipping check");
			return env;
		}

		long iat = OIDFJSON.getLong(iatEl);

		try {
			JWTUtil.validateIatClaim(iat);
		} catch (IllegalArgumentException e) {
			throw error("Client attestation JWT iat claim is invalid: " + e.getMessage(), args("iat", iat));
		}

		logSuccess("Client attestation JWT iat claim is valid", args("iat", iat));

		return env;
	}
}
