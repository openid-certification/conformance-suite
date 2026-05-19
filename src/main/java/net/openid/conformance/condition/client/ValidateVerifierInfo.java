package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractJsonSchemaBasedValidation;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

public class ValidateVerifierInfo extends AbstractJsonSchemaBasedValidation {

	@Override
	protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
		JsonObject wrapper = env.getObject(ExtractVerifierInfoFromClientConfiguration.ENV_KEY);
		return new JsonSchemaValidationInput("verifier_info",
			"json-schemas/oid4vp/verifier_info.json", wrapper);
	}

	@Override
	@PreEnvironment(required = ExtractVerifierInfoFromClientConfiguration.ENV_KEY)
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
}
