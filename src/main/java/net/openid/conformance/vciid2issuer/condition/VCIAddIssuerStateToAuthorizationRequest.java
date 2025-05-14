package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIAddIssuerStateToAuthorizationRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String issuerState = env.getString("vci","issuer_state");
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		authorizationEndpointRequest.addProperty("issuer_state", issuerState);
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		// TODO add issuer_state
			/*
			if we have issuer_state in the credential offer, add it to the authorization request
Check if credential_configuration_ids needs to be added to RAR object
			 */

		return env;
	}
}
