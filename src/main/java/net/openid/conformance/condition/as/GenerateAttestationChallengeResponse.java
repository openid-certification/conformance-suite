package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

public class GenerateAttestationChallengeResponse extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "attestation_challenge")
	@PostEnvironment(required = {"attestation_challenge_response", "attestation_challenge_response_headers"})
	public Environment evaluate(Environment env) {

		String challenge = env.getString("attestation_challenge");

		JsonObject headers = new JsonObject();
		headers.addProperty(HttpHeaders.CONTENT_TYPE, "application/json");
		headers.addProperty(HttpHeaders.CACHE_CONTROL, "no-store");

		JsonObject response = new JsonObject();
		response.addProperty("attestation_challenge", challenge);

		env.putObject("attestation_challenge_response", response);
		env.putObject("attestation_challenge_response_headers", headers);

		logSuccess("Created attestation challenge response",
			args("attestation_challenge_response", response,
				"attestation_challenge_response_headers", headers));

		return env;
	}
}
