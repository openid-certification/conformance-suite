package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class AddHintToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String hintType = env.getString("config", "client.hint_type");
		String hintValue = env.getString("config", "client.hint_value");
		List<String> hintTypeList = ImmutableList.of("login_hint_token", "id_token_hint", "login_hint");

		if (!hintTypeList.contains(hintType)) {
			throw error("the 'hint_type' provided in the configuration must be one of 'login_hint_token', 'id_token_hint' or 'login_hint'");
		}

		if (Strings.isNullOrEmpty(hintValue)) {
			throw error("the 'hint_value' provided in the configuration must not empty");
		}

		authorizationEndpointRequest.addProperty(hintType, hintValue);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added hint to authorization endpoint request", args(hintType, hintValue));

		return env;
	}

}
