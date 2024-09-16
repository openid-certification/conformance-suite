package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIsoMdocClientMetadataToAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "client_public_jwks"})
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		var clientMetaData = (JsonObject) JsonParser.parseString("""
{
	"require_signed_request_object": true,
	"vp_formats": {
	  "mso_mdoc": {
		"alg": [
		  "ES256"
		]
	  }
	}
}
""");

		authorizationEndpointRequest.add("client_metadata", clientMetaData);

		log("Added client_metadata to authorization endpoint request", args("client_metadata", clientMetaData));

		return env;
	}
}
