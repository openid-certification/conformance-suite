package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CheckForUnexpectedParametersInPostLogoutRedirect extends AbstractCondition {

	private static final List<String> EXPECTED_PARAMS = ImmutableList.of("state");

	@Override
	@PreEnvironment(required = "post_logout_redirect")
	public Environment evaluate(Environment env) {

		JsonObject params = env.getElementFromObject("post_logout_redirect", "query_string_params").getAsJsonObject();

		JsonObject unexpectedParams = new JsonObject();

		params.entrySet().forEach(entry -> {
			if (!EXPECTED_PARAMS.contains(entry.getKey())) {
				unexpectedParams.add(entry.getKey(), entry.getValue());
			}
		});

		if (unexpectedParams.size() != 0) {
			throw error("post_logout_redirect includes unexpected parameters in url query. This may be because the server supports extensions the test suite is unaware of, or the server may be sending values it should not.", unexpectedParams);
		}

		logSuccess("post_logout_redirect includes only expected parameters", params);

		return env;
	}

}
