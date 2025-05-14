package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIAddIssuerStateToAuthorizationRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String issuerState = "dummy_issuer_state"; // env.getString("vci","issuer_state");
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		authorizationEndpointRequest.addProperty("issuer_state", issuerState);
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		// TODO check if we have issuer_state in the credential offer,
		//  add it to the authorization request Check if credential_configuration_ids
		//  needs to be added to RAR object

		logSuccess("Added issuer_state to authorization_endpoint_request", args("isser_state", issuerState));

		return env;
	}
}
