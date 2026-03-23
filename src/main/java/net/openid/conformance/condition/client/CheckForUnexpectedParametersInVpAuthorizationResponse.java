package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CheckForUnexpectedParametersInVpAuthorizationResponse extends AbstractCondition {

	// https://openid.net/specs/openid-4-verifiable-presentations-1_0-29.html#section-8
	private static final List<String> EXPECTED_PARAMS = ImmutableList.of(
		"vp_token",
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
			throw error("authorization response includes unexpected parameters. This may indicate the wallet has misunderstood the spec, or it may be using extensions the test suite is unaware of.", unexpectedParams);
		}

		return env;
	}

}
