package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaException;
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

import java.util.List;

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
			throw error("A schema in the '" + schemaDescription + "' field in the test configuration is not a JSON object",
				args("schema", schemaElement));
		}
		JsonNode dataNode;
		try {
			dataNode = MAPPER.readTree(data.toString());
		} catch (JacksonException e) {
			throw error("Failed to parse JSON", e);
		}
		// The schema is user-supplied test configuration, so what the validator library throws while
		// making sense of it is a configuration problem, not a suite bug: SchemaException covers a
		// missing/unrecognized $schema, an invalid "pattern" regex, and (as InvalidSchemaRefException)
		// a $ref that cannot be resolved or fetched. Keep this block free of error() calls so the
		// ConditionError raised for a genuine validation failure below cannot be caught here and
		// relabelled as an unusable schema.
		List<Error> errors;
		try {
			JsonNode schemaNode = MAPPER.readTree(schemaElement.toString());
			SpecificationVersion specVersion = SpecificationVersionDetector.detect(schemaNode);
			SchemaRegistry registry = JsonSchemaValidation.createRegistry(specVersion,
				AbstractEkycSchemaBasedValidation.ekycSchemaMapperCustomizer());
			Schema schema = registry.getSchema(schemaNode);
			errors = schema.validate(dataNode);
		} catch (SchemaException | JacksonException e) {
			throw error("A schema in the '" + schemaDescription + "' field in the test configuration is not a usable JSON schema",
				e, args("schema", schemaElement));
		}
		if (!errors.isEmpty()) {
			JsonSchemaValidationResult result = new JsonSchemaValidationResult(errors);
			throw error("Failed to validate data against a schema from the '" + schemaDescription + "' field in the test configuration",
				args("schema", schemaElement, "data", data, "errors", result.getPropertyErrors()));
		}
	}
}
