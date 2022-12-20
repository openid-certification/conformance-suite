package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public abstract class AbstractValidateSuccessfulResponseFromAuthorizationEndpoint extends AbstractCondition {

	protected abstract List<String> getExpectedParams();

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		List<String> expectedParams = getExpectedParams();

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		JsonObject unexpectedParams = new JsonObject();

		callbackParams.entrySet().forEach(entry -> {
			if (!expectedParams.contains(entry.getKey())) {
				unexpectedParams.add(entry.getKey(), entry.getValue());
			}
		});

		if (unexpectedParams.size() == 0) {
			logSuccess("authorization endpoint response does not include unexpected parameters", callbackParams);
		} else {
			throw error("authorization endpoint response includes unexpected parameters. This may be because the authorization server supports protocol extensions the conformance suite is unaware of, but may also be because the server is implementing the specification incorrectly.", unexpectedParams);
		}

		return env;
	}

}
