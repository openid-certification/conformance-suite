package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.support.networknt.SpecificationVersionDetector;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.validation.JsonSchemaValidation;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base class for the conditions that validate eKYC verified claims (from the request or the
 * response) against the user-supplied custom JSON schemas in the test configuration. Subclasses
 * extract the claims to validate in {@link #evaluate(Environment)} and call
 * {@link #validateAgainstCustomSchema(JsonObject, JsonElement, String)} per configured schema.
 */
public abstract class AbstractValidateVerifiedClaimsAgainstCustomSchemas extends AbstractCondition {

	private static final ObjectMapper MAPPER = new JsonMapper();

	protected void validateAgainstCustomSchema(JsonObject data, JsonElement schemaElement, String schemaDescription) {
		if (!schemaElement.isJsonObject()) {
			throw error("Schema element is not a JSON object", args("schema", schemaElement));
		}
		try {
			JsonNode schemaNode = MAPPER.readTree(schemaElement.toString());
			SpecificationVersion specVersion = SpecificationVersionDetector.detect(schemaNode);
			SchemaRegistry registry = JsonSchemaValidation.createRegistry(specVersion,
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
