package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks the raw authorization endpoint HTTP request before it is merged with any fetched Request
 * Object claims. This is distinct from CheckForUnexpectedParametersInVpAuthorizationRequest, which
 * operates on the effective merged parameter set.
 */
public class CheckForUnexpectedParametersInVpAuthorizationEndpointHttpRequest extends AbstractCondition {

	public static final List<String> expectedRequestUriHttpRequestParams = List.of(
		"client_id",
		"request_uri",
		"request_uri_method"
	);

	public static final List<String> expectedRequestHttpRequestParams = List.of(
		"client_id",
		"request"
	);

	@Override
	@PreEnvironment(required = "authorization_endpoint_http_request_params")
	public Environment evaluate(Environment env) {
		JsonObject authParameters = env.getObject("authorization_endpoint_http_request_params");
		List<String> expectedParams;

		if (authParameters.has("request_uri")) {
			expectedParams = expectedRequestUriHttpRequestParams;
		} else if (authParameters.has("request")) {
			expectedParams = expectedRequestHttpRequestParams;
		} else {
			logSuccess("HTTP authorization request does not use request or request_uri; raw outer-parameter check is not applicable");
			return env;
		}

		List<String> unknownParameters = new ArrayList<>();
		for (String param : authParameters.keySet()) {
			if (expectedParams.contains(param)) {
				continue;
			}
			unknownParameters.add(param);
		}

		if (unknownParameters.isEmpty()) {
			logSuccess("All HTTP authorization request parameters are expected",
				args("parameters", authParameters.keySet(), "known_params", expectedParams));
		} else {
			throw error("Unknown parameters were found in the HTTP authorization request. This may indicate the verifier has misunderstood the spec, or it may be sending values in the front-channel that belong only in the fetched Request Object.",
				args("parameters", authParameters.keySet(), "unknown_params", unknownParameters, "known_params", expectedParams));
		}

		return env;
	}
}
