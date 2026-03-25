package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateClientAttestationProofJwtChallenge extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_attestation_pop_object")
	public Environment evaluate(Environment env) {

		String expectedChallenge = env.getString("attestation_challenge");
		if (expectedChallenge == null) {
			// No challenge endpoint was used, skip validation
			log("No attestation challenge issued, skipping challenge validation");
			return env;
		}

		JsonObject clientAttestationPopObj = env.getObject("client_attestation_pop_object");
		if (clientAttestationPopObj == null) {
			throw error("missing client_attestation_pop_object");
		}

		JsonElement challengeEl = env.getElementFromObject("client_attestation_pop_object", "claims.challenge");
		if (challengeEl == null) {
			throw error("challenge claim missing in client attestation PoP JWT. "
				+ "When the authorization server provides a challenge_endpoint, "
				+ "the client MUST fetch a challenge and include it as the challenge claim.",
				args("client_attestation_pop_object", clientAttestationPopObj));
		}

		String challenge = OIDFJSON.getString(challengeEl);
		if (!challenge.equals(expectedChallenge)) {
			throw error("challenge claim in client attestation PoP JWT does not match the issued attestation challenge",
				args("expected", expectedChallenge, "actual", challenge,
					"client_attestation_pop_object", clientAttestationPopObj));
		}

		logSuccess("Client attestation PoP JWT challenge matches issued attestation challenge",
			args("challenge", challenge));

		return env;
	}
}
