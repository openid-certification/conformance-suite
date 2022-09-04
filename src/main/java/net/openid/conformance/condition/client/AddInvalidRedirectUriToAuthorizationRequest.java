package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class AddInvalidRedirectUriToAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request", strings = "redirect_uri" )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString("redirect_uri");
		String invalidUri = UriComponentsBuilder.fromUriString(redirectUri)
				.path("_invalid")
				.toUriString();

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		authorizationEndpointRequest.remove("redirect_uri");
		authorizationEndpointRequest.addProperty("redirect_uri", invalidUri);

		logSuccess("Added invalid redirect_uri to authorization endpoint request", args("redirect_uri", invalidUri));

		return env;
	}

}
