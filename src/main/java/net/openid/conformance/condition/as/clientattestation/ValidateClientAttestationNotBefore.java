package net.openid.conformance.condition.as.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

/**
 * Validates the client attestation JWT's 'nbf' claim if present. Per OAuth 2.0
 * Attestation-Based Client Authentication §5.1, 'nbf' is OPTIONAL — the condition skips
 * when it is absent. When present, 'nbf' must be a reasonable unix timestamp, not too
 * far in the past (catching millisecond-timestamp or epoch-default bugs), and not in
 * the future (allowing clock skew).
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.1">OAuth 2.0 Attestation-Based Client Authentication §5.1</a>
 */
public class ValidateClientAttestationNotBefore extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_attestation_object")
	public Environment evaluate(Environment env) {

		JsonElement nbfEl = env.getElementFromObject("client_attestation_object", "claims.nbf");

		if (nbfEl == null) {
			log("Client attestation JWT has no nbf claim (optional), skipping check");
			return env;
		}

		long nbf = OIDFJSON.getLong(nbfEl);

		try {
			JWTUtil.validateNbfClaim(nbf);
		} catch (IllegalArgumentException e) {
			throw error("Client attestation JWT nbf claim is invalid: " + e.getMessage(), args("nbf", nbf));
		}

		logSuccess("Client attestation JWT nbf claim is valid", args("nbf", nbf));

		return env;
	}
}
