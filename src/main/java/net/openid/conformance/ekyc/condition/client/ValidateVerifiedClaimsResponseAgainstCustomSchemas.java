package net.openid.conformance.ekyc.condition.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.support.networknt.SpecificationVersionDetector;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;
import tools.jackson.core.JacksonException;

public class ValidateVerifiedClaimsResponseAgainstCustomSchemas extends AbstractCondition {

	private static final ObjectMapper MAPPER = new JsonMapper();

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
			SpecificationVersion specVersion = SpecificationVersionDetector.detect(schemaNode);
			SchemaRegistry registry = SchemaRegistry.withDefaultDialect(specVersion,
				AbstractEkycSchemaBasedValidation.ekycSchemaMapperCustomizer());
			Schema schema = registry.getSchema(schemaNode);
			JsonNode dataNode = MAPPER.readTree(data.toString());
			var errors = schema.validate(dataNode);
			if (!errors.isEmpty()) {
				JsonSchemaValidationResult result = new JsonSchemaValidationResult(errors);
				throw error("Failed to validate data against " + schemaDescription,
					args("schema", schemaElement, "data", data, "errors", result.getPropertyErrors()));
			}
		} catch (JacksonException e) {
			throw error("Failed to parse JSON", e);
		}
	}
}
