package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractJsonSchemaBasedValidation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

public class VCICredentialIssuerMetadataValidation extends AbstractJsonSchemaBasedValidation {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();
		String schemaResource = "json-schemas/oid4vci/credential_issuer_metadata-1_0.json";
		String metadataName = "OID4VCI Credential Issuer metadata";
		return new JsonSchemaValidationInput(metadataName, schemaResource, metadata);
	}

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		JsonSchemaValidationResult structuralErrors = validationResult.withoutUnknownPropertyErrors();
		if (!structuralErrors.isValid()) {
			super.onValidationFailure(env, structuralErrors, input);
		}
	}

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
