package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CheckForUnexpectedParametersInFrontchannelLogoutRequest extends AbstractCondition {

	private static final List<String> EXPECTED_PARAMS = ImmutableList.of("sid", "iss");

	@Override
	@PreEnvironment(required = "frontchannel_logout_request")
	public Environment evaluate(Environment env) {

		JsonObject callbackParams = env.getElementFromObject("frontchannel_logout_request", "query_string_params").getAsJsonObject();

		JsonObject unexpectedParams = new JsonObject();

		callbackParams.entrySet().forEach(entry -> {
			if (!EXPECTED_PARAMS.contains(entry.getKey())) {
				unexpectedParams.add(entry.getKey(), entry.getValue());
			}
		});

		if (unexpectedParams.size() != 0) {
			throw error("frontchannel_logout_request includes unexpected parameters. This may be because the server supports extensions the test suite is unaware of, or the server may be sending values it should not.", unexpectedParams);
		}

		logSuccess("frontchannel_logout_request includes only expected parameters", callbackParams);

		return env;
	}

}
