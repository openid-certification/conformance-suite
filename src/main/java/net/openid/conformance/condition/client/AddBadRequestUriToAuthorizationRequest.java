package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

//This class adds an invalid request_uri to the request object
//PAR-2.1 : The request_uri authorization request parameter MUST NOT be provided in this case
public class AddBadRequestUriToAuthorizationRequest extends AbstractCondition {

	private static final String BAD_REQUEST_URI_VALUE = "urn%3Aexample%3Abwc4JK-ESC0w8acc191e-Y1LTC2";
	private static final String REQUEST_URI_KEY = "request_uri";

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty(REQUEST_URI_KEY, BAD_REQUEST_URI_VALUE);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added bad request_uri to request object", args(REQUEST_URI_KEY, BAD_REQUEST_URI_VALUE));

		return env;
	}
}
