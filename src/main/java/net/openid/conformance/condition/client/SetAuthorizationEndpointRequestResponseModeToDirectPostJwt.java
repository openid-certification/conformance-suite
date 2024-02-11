package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetAuthorizationEndpointRequestResponseModeToDirectPostJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("response_mode", "direct_post.jwt");
		authorizationEndpointRequest.addProperty("client_id_scheme", "x509_san_uri"); // FIXME: use x509_san_dns instead, as per the only one that's supported B.3.1.3.1B.3.1.3.1	Static set of Wallet Metadata in IOS 18013-7

		log("Added response_mode parameter to request", authorizationEndpointRequest);

		return env;
	}

}
