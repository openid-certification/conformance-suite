package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateIntrospectionRequestForRefreshToken extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "refresh_token")
	@PostEnvironment(required = "introspection_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = new JsonObject();
		o.addProperty("token", env.getString("refresh_token"));
		o.addProperty("token_type_hint", "refresh_token");

		env.putObject("introspection_endpoint_request_form_parameters", o);

		logSuccess("Created token introspection request for the refresh token", o);

		return env;
	}

}
