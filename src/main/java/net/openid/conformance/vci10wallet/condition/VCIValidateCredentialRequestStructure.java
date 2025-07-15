package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10issuer.condition.AbstractJsonSchemaBasedValidation;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationInput;

/**
 * See: 8.2. Credential Request https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2
 */
public class VCIValidateCredentialRequestStructure extends AbstractJsonSchemaBasedValidation {

	@Override
	public Environment evaluate(Environment env) {

		JsonElement bodyJson = env.getElementFromObject("incoming_request", "body_json");
		if (bodyJson == null) {
			throw error("Failed to detected json payload in incoming request.", args("incoming_request", env.getObject("incoming_request")));
		}

		JsonObject credentialRequestBodyJson = bodyJson.getAsJsonObject();

		JsonElement credentialIdentifier = credentialRequestBodyJson.get("credential_identifier");
		JsonElement credentialConfigId = credentialRequestBodyJson.get("credential_configuration_id");
		if (credentialIdentifier != null && credentialConfigId != null) {
			throw error("credential_identifier and credential_configuration_id are mutually exclusive", args("credential_request", credentialRequestBodyJson));
		}

		JsonElement proof = credentialRequestBodyJson.get("proof");
		JsonElement proofs = credentialRequestBodyJson.get("proofs");
		if (proof != null && proofs != null) {
			throw error("proof and proofs are mutually exclusive", args("credential_request", credentialRequestBodyJson));
		}

		// perform JSON schema based validation
		return super.evaluate(env);
	}

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject credentialRequestBodyJson = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		String schemaResource = "json-schemas/oid4vci/credential_request-1_0.json";
		String metadataName = "OID4VCI Credential Request";
		return new JsonSchemaValidationInput(metadataName, schemaResource, credentialRequestBodyJson);
	}

}
