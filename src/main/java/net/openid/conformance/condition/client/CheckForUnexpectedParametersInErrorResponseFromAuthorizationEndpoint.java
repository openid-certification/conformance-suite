package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint extends AbstractCondition {

	private static final List<String> EXPECTED_PARAMS = ImmutableList.of(
		"error",
		"error_description",
		"error_uri",
		"state",
		"session_state",
		"iss" // https://tools.ietf.org/html/draft-ietf-oauth-iss-auth-resp
	);

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		// https://openid.net/specs/openid-connect-core-1_0.html#AuthError
		if (callbackParams.has("error")) {

			JsonObject unexpectedParams = new JsonObject();

			callbackParams.entrySet().forEach(entry -> {
				if (!EXPECTED_PARAMS.contains(entry.getKey())) {
					unexpectedParams.add(entry.getKey(), entry.getValue());
				}
			});

			if (unexpectedParams.size() == 0) {
				logSuccess("error response includes only expected parameters", callbackParams);
			} else {
				throw error("error response includes unexpected parameters. This may be because the server supports extensions the test suite is unaware of, or the server may be returning values it should not.", unexpectedParams);
			}
		} else {
			throw error("Authorization server was expected to return an error but did not", callbackParams);
		}
		return env;
	}

}
