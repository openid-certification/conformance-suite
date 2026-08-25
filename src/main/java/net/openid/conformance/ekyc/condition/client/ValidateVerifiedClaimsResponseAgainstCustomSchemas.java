package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateVerifiedClaimsResponseAgainstCustomSchemas extends AbstractValidateVerifiedClaimsAgainstCustomSchemas {

	@Override
	@PreEnvironment(required = {"verified_claims_response"})
	public Environment evaluate(Environment env) {
		JsonElement responseSchemas = env.getElementFromObject("config", "ekyc.response_schemas");
		if (responseSchemas == null) {
			logSuccess("No custom response schemas configured");
			return env;
		}

		JsonObject claimsObject = AbstractEkycSchemaBasedValidation.extractAndWrapResponseClaims(env);
		if (claimsObject == null) {
			logSuccess("No verified claims to validate against custom schemas");
			return env;
		}

		for (JsonElement schemaElement : OIDFJSON.packJsonElementIntoJsonArray(responseSchemas)) {
			validateAgainstCustomSchema(claimsObject, schemaElement, "eKYC Additional Response Validation Schemas");
		}

		logSuccess("Validated response claims against custom schemas");
		return env;
	}
}
