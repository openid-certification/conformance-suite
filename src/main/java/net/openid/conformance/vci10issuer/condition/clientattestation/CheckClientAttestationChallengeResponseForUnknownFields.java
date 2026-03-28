package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public class CheckClientAttestationChallengeResponseForUnknownFields extends AbstractCondition {

	private static final Set<String> KNOWN_FIELDS = Set.of("attestation_challenge");

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		String responseBody = OIDFJSON.getString(env.getElementFromObject("endpoint_response", "body"));
		JsonObject responseObject = JsonParser.parseString(responseBody).getAsJsonObject();

		Set<String> unknownFields = new HashSet<>(responseObject.keySet());
		unknownFields.removeAll(KNOWN_FIELDS);
		if (!unknownFields.isEmpty()) {
			// The spec says "The Authorization Server MAY add additional challenges or data",
			// so additional fields are allowed but worth flagging for sender validation
			throw error("Challenge endpoint response contains unknown fields",
				args("unknown_fields", unknownFields, "challenge_response", responseObject));
		}

		logSuccess("Challenge endpoint response contains no unknown fields");
		return env;
	}
}
