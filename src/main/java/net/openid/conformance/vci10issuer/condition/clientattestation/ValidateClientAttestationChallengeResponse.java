package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateClientAttestationChallengeResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	@PostEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		String responseBody = OIDFJSON.getString(env.getElementFromObject("endpoint_response", "body"));
		JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();
		if (!responseObject.has("attestation_challenge")) {
			throw error("Could not find attestation_challenge in challenge endpoint response",
				args("challenge_response", responseObject));
		}

		String challenge = OIDFJSON.getString(responseObject.get("attestation_challenge"));

		env.putString("vci", "attestation_challenge", challenge);

		logSuccess("Found valid attestation challenge response",
			args("attestation_challenge", challenge, "challenge_response", responseObject));
		return env;
	}
}
