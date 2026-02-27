package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddConnectIDHintToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String hintType = env.getString("config", "client.hint_type");
		String hintValue = env.getString("config", "client.hint_value");

		if (!"login_hint".equals(hintType)) {
			throw error("For ConnectID, 'hint_type' must be 'login_hint'");
		}

		if (hintValue == null) {
			throw error("the 'hint_value' provided in the configuration must not be empty");
		}

		try {
			JsonElement hintValueJson = JsonParser.parseString(hintValue);
			authorizationEndpointRequest.add(hintType, hintValueJson);
		} catch (JsonSyntaxException e) {
			// If it's not valid JSON, treat it as a string
			authorizationEndpointRequest.addProperty(hintType, hintValue);
		}

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added ConnectID hint to authorization endpoint request", args(hintType, authorizationEndpointRequest.get(hintType)));

		return env;
	}

}
