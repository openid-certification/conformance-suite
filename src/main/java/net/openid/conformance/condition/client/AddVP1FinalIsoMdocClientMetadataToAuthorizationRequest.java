package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddVP1FinalIsoMdocClientMetadataToAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "client_public_jwks"})
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		JsonObject vpFormatsSupported = new JsonObject();
		vpFormatsSupported.add("mso_mdoc", createMsoMdocFormatParameters(env));
		JsonObject clientMetaData = new JsonObject();
		clientMetaData.add("vp_formats_supported", vpFormatsSupported);

		authorizationEndpointRequest.add("client_metadata", clientMetaData);

		log("Added client_metadata to authorization endpoint request", args("client_metadata", clientMetaData));

		return env;
	}

	protected JsonObject createMsoMdocFormatParameters(Environment env) {
		return (JsonObject) JsonParser.parseString("""
			{
				"issuerauth_alg_values": [ -7 ]
			}
			""");
	}
}
