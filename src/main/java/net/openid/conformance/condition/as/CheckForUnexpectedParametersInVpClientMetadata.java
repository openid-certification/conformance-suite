package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckForUnexpectedParametersInVpClientMetadata extends AbstractCondition {
	public static List<String> expectedClientMetadata = List.of(
		"jwks",
		"vp_formats",
		"authorization_signed_response_alg",
		"authorization_encrypted_response_alg",
		"authorization_encrypted_response_enc"
		);

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {
		JsonElement clientMetadata = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata");
		if (clientMetadata == null) {
			// It's mandatory except when vp_formats is "available to the Wallet via another mechanism", and our
			// "fake wallet" doesn't currently support any other mechanisms
			throw error("client_metadata not present in request but it is mandatory to include vp_formats in it");
		}
		if (!clientMetadata.isJsonObject()) {
			throw error("client_metadata is not a JSON object", args("client_metadata", clientMetadata));
		}

		List<String> unknownParameters = new ArrayList<>();

		for (String claim : clientMetadata.getAsJsonObject().keySet()) {
			if (expectedClientMetadata.contains(claim)) {
				continue;
			}
			unknownParameters.add(claim);
		}

		if (unknownParameters.isEmpty()) {
			logSuccess("All client_metadata parameters are expected",
				args("parameters", clientMetadata.getAsJsonObject().keySet()));
		} else {
			throw error("Unknown parameters were found in the client_metadata. This may indicate the verifier has misunderstood the spec, or it may be using extensions the test suite is unaware of. The working group clarified what can be used in client_metadata in https://github.com/openid/OpenID4VP/pull/233",
				args("parameters", clientMetadata.getAsJsonObject().keySet(), "unknown_params", unknownParameters));
		}

		return env;
	}
}
