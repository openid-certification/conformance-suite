package net.openid.conformance.condition.as.par;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnsureAuthorizationRequestContainsOnlyExpectedParamsWhenUsingPAR extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_http_request_params"})
	public Environment evaluate(Environment env) {
		List<String> expectedParams = Arrays.asList(new String[]{"client_id", "request_uri"});
		JsonObject paramsObject = env.getObject("authorization_endpoint_http_request_params");
		ArrayList<String> unexpectedParams = new ArrayList<>();

		for (String key: paramsObject.keySet()) {
			if (! expectedParams.contains(key)) {
				unexpectedParams.add(key);
			}
		}

		if (! unexpectedParams.isEmpty()) {
			throw error("Authorization request contains unexpected parameters when using PAR. Only 'client_id' and 'request_uri' " +
				"should be present, The inclusion of other parameters may result in data being unintentionally leaked to the " +
				"browser or in logs",
				args("unexpected params", unexpectedParams));
		}

		logSuccess("Request does not contain any unexpected parameters");

		return env;
	}

}
