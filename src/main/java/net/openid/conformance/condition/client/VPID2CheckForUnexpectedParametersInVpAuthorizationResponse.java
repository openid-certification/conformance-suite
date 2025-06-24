package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class VPID2CheckForUnexpectedParametersInVpAuthorizationResponse extends AbstractCondition {

	// https://openid.net/specs/openid-4-verifiable-presentations-1_0-ID2.html#section-6.1
	private static final List<String> EXPECTED_PARAMS = ImmutableList.of(
		"vp_token",
		"presentation_submission",
		"state",
		"iss"
	);

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		JsonObject unexpectedParams = new JsonObject();

		callbackParams.entrySet().forEach(entry -> {
			if (!EXPECTED_PARAMS.contains(entry.getKey())) {
				unexpectedParams.add(entry.getKey(), entry.getValue());
			}
		});

		if (unexpectedParams.size() == 0) {
			logSuccess("authorization response includes only expected parameters", callbackParams);
		} else {
			throw error("authorization response includes unexpected parameters. This may be because the server supports extensions the test suite is unaware of, or the server may be returning values it should not, or returning values in an incorrect location.", unexpectedParams);
		}

		return env;
	}

}
