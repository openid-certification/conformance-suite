package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class VCITryAddingIssuerStateToAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"vci", "authorization_endpoint_request"})
	public Environment evaluate(Environment env) {

		String issuerState = env.getString("vci", "issuer_state");
		if (issuerState == null) {
			log("Could not find issuer_state");
			return env;
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		authorizationEndpointRequest.addProperty("issuer_state", issuerState);
		log("Added issuer_state to authorization_endpoint_request", args("issuer_state", issuerState));

		return env;
	}
}
