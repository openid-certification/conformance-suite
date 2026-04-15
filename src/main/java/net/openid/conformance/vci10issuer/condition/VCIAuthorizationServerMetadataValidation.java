package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractJsonSchemaBasedValidation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

public class VCIAuthorizationServerMetadataValidation extends AbstractJsonSchemaBasedValidation {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject authorizationServerMetadata = getAuthorizationServerMetadata(env);
		// we use the oid4vci specific variant of the rfc8414 metadata
		String schemaResource = "json-schemas/oid4vci/rfc8414-oauth_authorization_server_metadata.json";
		String inputName = "OAuth Authorization Server metadata";
		return new JsonSchemaValidationInput(inputName, schemaResource, authorizationServerMetadata);
	}

	protected JsonObject getAuthorizationServerMetadata(Environment env) {
		return env.getObject("server");
	}

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		JsonSchemaValidationResult structuralErrors = validationResult.withoutUnknownPropertyErrors();
		if (!structuralErrors.isValid()) {
			super.onValidationFailure(env, structuralErrors, input);
		}
	}

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
