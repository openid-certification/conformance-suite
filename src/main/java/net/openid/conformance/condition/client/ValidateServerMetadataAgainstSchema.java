package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractJsonSchemaBasedValidation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

/**
 * Validates the structure of authorization server / OpenID provider metadata (the {@code server}
 * environment object) against a superset JSON schema of RFC 8414 / OpenID Connect Discovery and the
 * various profile extensions. The schema is purely structural (types/formats of whatever fields are
 * present); only those errors are reported as failures. It deliberately does not require any field
 * (not even {@code issuer}): OpenID Federation OP metadata legitimately omits {@code issuer} because
 * the entity identifier is the issuer. Unknown properties are ignored here and instead surfaced as
 * warnings by {@link CheckForUnexpectedParametersInServerMetadata}. Suite-specific "field must be
 * present" requirements (including {@code issuer}) are left to the individual CheckDiscEndpoint* /
 * issuer-check conditions in each protocol's discovery verification.
 */
public class ValidateServerMetadataAgainstSchema extends AbstractJsonSchemaBasedValidation {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject serverMetadata = getServerMetadata(env);
		String schemaResource = "json-schemas/rfc8414/oauth_authorization_server_metadata.json";
		String inputName = "OAuth Authorization Server metadata";
		return new JsonSchemaValidationInput(inputName, schemaResource, serverMetadata);
	}

	protected JsonObject getServerMetadata(Environment env) {
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
