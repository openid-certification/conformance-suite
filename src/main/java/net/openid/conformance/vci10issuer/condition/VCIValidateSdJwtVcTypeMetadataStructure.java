package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractJsonSchemaBasedValidation;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

/**
 * Validates the structural shape of an SD-JWT VC Type Metadata document
 * against the JSON Schema modeling IETF SD-JWT VC draft-13 §6.2 and §9.
 * Unknown properties are filtered out here — they are surfaced separately by
 * {@link CheckForUnexpectedParametersInSdJwtVcTypeMetadata} so the caller can
 * give them WARNING severity while structural failures stay at FAILURE.
 */
public class VCIValidateSdJwtVcTypeMetadataStructure extends AbstractJsonSchemaBasedValidation {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
		JsonSchemaValidationResult structuralErrors = validationResult.withoutUnknownPropertyErrors();
		if (!structuralErrors.isValid()) {
			super.onValidationFailure(env, structuralErrors, input);
		}
	}

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject typeMetadata = env.getElementFromObject("vci", "sdjwt_vc_type_metadata").getAsJsonObject();
		return new JsonSchemaValidationInput("SD-JWT VC Type Metadata",
			"json-schemas/sdjwtvc/type_metadata.json", typeMetadata);
	}
}
