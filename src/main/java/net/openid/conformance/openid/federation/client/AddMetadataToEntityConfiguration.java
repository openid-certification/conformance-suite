package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddMetadataToEntityConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		JsonObject metadata = new JsonObject();

		JsonObject federationEntity = new JsonObject();
		metadata.add("federation_entity", federationEntity);

		JsonObject openIdProvider = new JsonObject();
		metadata.add("openid_provider", openIdProvider);

		JsonArray responseTypesSupported = new JsonArray();
		responseTypesSupported.add("code");
		openIdProvider.add("response_types_supported", responseTypesSupported);

		JsonArray subjectTypesSupported = new JsonArray();
		subjectTypesSupported.add("public");
		openIdProvider.add("subject_types_supported", subjectTypesSupported);

		JsonArray idTokenSigningAlgsSupported = new JsonArray();
		idTokenSigningAlgsSupported.add("RS256");
		openIdProvider.add("id_token_signing_alg_values_supported", idTokenSigningAlgsSupported);

		String entityIdentifier = OIDFJSON.getString(server.get("iss"));
		openIdProvider.addProperty("authorization_endpoint", entityIdentifier + "/authorize");
		openIdProvider.addProperty("token_endpoint", entityIdentifier + "/token");
		openIdProvider.addProperty("jwks_uri", entityIdentifier + "/jwks");

		JsonArray clientRegistrationTypesSupported = new JsonArray();
		clientRegistrationTypesSupported.add("automatic");
		openIdProvider.add("client_registration_types_supported", clientRegistrationTypesSupported);

		server.add("metadata", metadata);

		logSuccess("Added openid_provider metadata to server configuration", args("server", server));

		return env;
	}

}
