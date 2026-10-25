package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public class VCICheckCredentialNonceResponseForUnknownFields extends AbstractCondition {

	private static final Set<String> KNOWN_FIELDS = Set.of("c_nonce");

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		String responseBody = OIDFJSON.getString(env.getElementFromObject("endpoint_response", "body"));
		JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();

		Set<String> unknownFields = new HashSet<>(responseObject.keySet());
		unknownFields.removeAll(KNOWN_FIELDS);
		String challengeEndpoint = env.getString("server", "challenge_endpoint");
		String nonceEndpoint = env.getString("vci", "credential_issuer_metadata.nonce_endpoint");
		if (challengeEndpoint != null && challengeEndpoint.equals(nonceEndpoint)) {
			unknownFields.remove("attestation_challenge");
		}
		if (!unknownFields.isEmpty()) {
			throw error("Nonce endpoint response contains unknown fields",
				args("unknown_fields", unknownFields, "nonce_response", responseObject));
		}

		logSuccess("Nonce endpoint response contains no unknown fields");
		return env;
	}
}
