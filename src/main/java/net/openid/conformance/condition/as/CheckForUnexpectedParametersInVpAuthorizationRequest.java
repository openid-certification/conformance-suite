package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckForUnexpectedParametersInVpAuthorizationRequest extends AbstractCondition {
	public static List<String> expectedAuthRequestParams = List.of(
		"presentation_definition",
		"dcql_query",
		"client_id",
		"client_id_scheme",
		"response_uri",
		"client_metadata",
		"nonce",
		"state",
		"response_type",
		"response_mode",
		// these are legal, and we have separate conditions that deal with them:
		"scope", // not valid when using dcql_query
		"request_uri_method", // not supported by suite (but it is legal to ignore it)
		// not really authorization parameters but these appear when we unpack the request object
		"aud",
		"iss",
		"iat",
		"nbf",
		"exp",
		"jti"
		);

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {
		JsonObject authParameters = env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);

		List<String> unknownParameters = new ArrayList<>();

		for (String claim : authParameters.keySet()) {
			if (expectedAuthRequestParams.contains(claim)) {
				continue;
			}
			unknownParameters.add(claim);
		}

		if (unknownParameters.isEmpty()) {
			logSuccess("All authorization parameters are expected",args("parameters", authParameters.keySet(), "known_params", expectedAuthRequestParams));
		} else {
			throw error("Unknown parameters were found in the authorization request. This may indicate the verifier has misunderstood the spec, or it may be using extensions the test suite is unaware of.", args("parameters", authParameters.keySet(), "unknown_params", unknownParameters, "known_params", expectedAuthRequestParams));
		}

		return env;
	}
}
