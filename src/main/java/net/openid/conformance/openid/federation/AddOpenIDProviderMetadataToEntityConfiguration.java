package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddOpenIDProviderMetadataToEntityConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		JsonObject metadata = server.getAsJsonObject("metadata");

		JsonObject openIdProvider = new JsonObject();
		metadata.add("openid_provider", openIdProvider);

		JsonArray scopesSupported = new JsonArray();
		scopesSupported.add("openid");
		openIdProvider.add("scopes_supported", scopesSupported);

		JsonArray claimsSupported = new JsonArray();
		claimsSupported.add("sub");
		openIdProvider.add("claims_supported", claimsSupported);

		JsonArray responseTypesSupported = new JsonArray();
		responseTypesSupported.add("code");
		openIdProvider.add("response_types_supported", responseTypesSupported);

		JsonArray subjectTypesSupported = new JsonArray();
		subjectTypesSupported.add("public");
		openIdProvider.add("subject_types_supported", subjectTypesSupported);

		JsonArray idTokenSigningAlgsSupported = new JsonArray();
		idTokenSigningAlgsSupported.add("RS256");
		idTokenSigningAlgsSupported.add("PS256");
		idTokenSigningAlgsSupported.add("ES256");
		openIdProvider.add("id_token_signing_alg_values_supported", idTokenSigningAlgsSupported);

		JsonArray requestObjectSigningAlgValuesSupported = new JsonArray();
		requestObjectSigningAlgValuesSupported.add("RS256");
		requestObjectSigningAlgValuesSupported.add("PS256");
		requestObjectSigningAlgValuesSupported.add("ES256");
		openIdProvider.add("request_object_signing_alg_values_supported", requestObjectSigningAlgValuesSupported);

		JsonArray requestObjectEncryptionAlgValuesSupported = new JsonArray();
		requestObjectEncryptionAlgValuesSupported.add("RSA-OAEP");
		openIdProvider.add("request_object_encryption_alg_values_supported", requestObjectEncryptionAlgValuesSupported);

		JsonArray requestObjectEncryptionEncValuesSupported = new JsonArray();
		requestObjectEncryptionEncValuesSupported.add("A256GCM");
		openIdProvider.add("request_object_encryption_enc_values_supported", requestObjectEncryptionEncValuesSupported);

		openIdProvider.addProperty("request_parameter_supported", true);
		openIdProvider.addProperty("request_uri_parameter_supported", true);
		openIdProvider.addProperty("require_signed_request_object", true);

		String entityIdentifier = OIDFJSON.getString(server.get("iss"));
		openIdProvider.addProperty("authorization_endpoint", entityIdentifier + "/authorize");
		openIdProvider.addProperty("pushed_authorization_request_endpoint", entityIdentifier + "/par");
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
