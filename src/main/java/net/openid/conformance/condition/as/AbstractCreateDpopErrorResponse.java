package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class  AbstractCreateDpopErrorResponse extends AbstractCondition {

	protected Environment createAuthorizationServerEndpointDpopErrorResponse(Environment env, String endpointResponseKey, String expectedNonce) {
		JsonObject endpointResponse = new JsonObject();
		endpointResponse.addProperty("error", "use_dpop_nonce");
		endpointResponse.addProperty("error_description", "Authorization server requires nonce in DPoP proof");

		env.putInteger(endpointResponseKey.concat("_http_status"), 400);

		JsonObject endpointResponseHeaders = new JsonObject();
		endpointResponseHeaders.addProperty("DPoP-Nonce", expectedNonce);
		env.putObject(endpointResponseKey.concat("_headers"), endpointResponseHeaders);

		env.putObject(endpointResponseKey, endpointResponse);
		return env;
	}

	protected Environment createResourceServerEndpointDpopErrorResponse(Environment env, String endpointResponseKey, String expectedNonce) {
		env.putInteger(endpointResponseKey.concat("_http_status"), 401);

		JsonObject endpointResponseHeaders = new JsonObject();
		endpointResponseHeaders.addProperty("WWW-Authenticate", "DPoP error=\"use_dpop_nonce\", error_description=\"Resource server requires nonce in DPoP proof\"");
		endpointResponseHeaders.addProperty("DPoP-Nonce", expectedNonce);
		env.putObject(endpointResponseKey.concat("_headers"), endpointResponseHeaders);

		return env;
	}
}
