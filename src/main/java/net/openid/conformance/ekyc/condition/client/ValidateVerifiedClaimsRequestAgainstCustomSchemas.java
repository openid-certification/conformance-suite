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
import net.openid.conformance.util.validation.JsonSchemaValidationResult;

import java.io.IOException;
import java.util.Set;

public class ValidateVerifiedClaimsRequestAgainstCustomSchemas extends AbstractCondition {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {
		JsonObject authorizationRequest = env.getObject("authorization_endpoint_request");
		if (!authorizationRequest.has("claims")) {
			logSuccess("No claims to validate against custom schemas");
			return env;
		}

		JsonElement requestSchemas = env.getElementFromObject("config", "ekyc.request_schemas");
		if (requestSchemas == null) {
			logSuccess("No custom request schemas configured");
			return env;
		}

		JsonObject claims = authorizationRequest.getAsJsonObject("claims");
		for (JsonElement schemaElement : OIDFJSON.packJsonElementIntoJsonArray(requestSchemas)) {
			validateAgainstCustomSchema(claims, schemaElement, "user-provided request schema");
		}

		logSuccess("Validated request claims against custom schemas");
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
