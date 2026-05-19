package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckForUnexpectedParametersInRequestUriPost extends AbstractCondition {

	// As per OID4VP 1.0 Final §5.10.1
	public static final List<String> expectedFormParams = List.of(
		"wallet_metadata",
		"wallet_nonce"
	);

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		JsonObject formParams = env.getElementFromObject("incoming_request", "body_form_params").getAsJsonObject();

		List<String> unknownParameters = new ArrayList<>();
		for (String key : formParams.keySet()) {
			if (!expectedFormParams.contains(key)) {
				unknownParameters.add(key);
			}
		}

		if (unknownParameters.isEmpty()) {
			logSuccess("All form parameters in request_uri POST body are expected",
				args("parameters", formParams.keySet(), "known_params", expectedFormParams));
		} else {
			throw error("Unknown form parameters were found in the request_uri POST body. The wallet may be using extensions the test suite is unaware of, or it may have misunderstood the spec.",
				args("parameters", formParams.keySet(),
					"unknown_params", unknownParameters,
					"known_params", expectedFormParams));
		}

		return env;
	}
}
