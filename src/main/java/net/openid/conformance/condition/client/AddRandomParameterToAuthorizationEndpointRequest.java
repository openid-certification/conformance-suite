package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class AddRandomParameterToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String parameter = RandomStringUtils.secure().nextAlphanumeric(16);
		String value = RandomStringUtils.secure().nextAlphanumeric(16);
		authorizationEndpointRequest.addProperty(parameter, value);

		logSuccess("Added a  authorization_endpoint_request. As per spec, 'The authorization server MUST ignore" +
				" unrecognized request parameters'.",
			args("authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}

}
