package net.openid.conformance.ekyc.condition.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.vci10issuer.util.JsonSchemaValidationResult;

import java.io.IOException;
import java.util.Set;

public class ValidateVerifiedClaimsResponseAgainstCustomSchemas extends AbstractCondition {

	private static final ObjectMapper MAPPER = new ObjectMapper();

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
			validateAgainstCustomSchema(claimsObject, schemaElement, "user-provided response schema");
		}

		logSuccess("Validated response claims against custom schemas");
		return env;
	}

	private void validateAgainstCustomSchema(JsonObject data, JsonElement schemaElement, String schemaDescription) {
		if (!schemaElement.isJsonObject()) {
			throw error("Schema element is not a JSON object", args("schema", schemaElement));
		}
		try {
			JsonNode schemaNode = MAPPER.readTree(schemaElement.toString());
			JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode),
				AbstractEkycSchemaBasedValidation.ekycSchemaMapperCustomizer());
			JsonSchema schema = factory.getSchema(schemaNode);
			JsonNode dataNode = MAPPER.readTree(data.toString());
			Set<ValidationMessage> errors = schema.validate(dataNode);
			if (!errors.isEmpty()) {
				JsonSchemaValidationResult result = new JsonSchemaValidationResult(errors);
				throw error("Failed to validate data against " + schemaDescription,
					args("schema", schemaElement, "data", data, "errors", result.getPropertyErrors()));
			}
		} catch (IOException e) {
			throw error("Failed to parse JSON", e);
		}
	}
}
