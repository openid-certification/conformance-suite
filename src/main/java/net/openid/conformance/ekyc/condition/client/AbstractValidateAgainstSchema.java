package net.openid.conformance.ekyc.condition.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.networknt.schema.ExecutionConfig;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public abstract class AbstractValidateAgainstSchema extends AbstractCondition {
	protected static final String ekycVerifiedClaimsRequestResourceFile = "json-schemas/ekyc-ida/12/verified_claims_request.json";
	protected static final String ekycVerifiedClaimsResourceFile = "json-schemas/ekyc-ida/12/verified_claims.json";

	private static ObjectMapper mapper = new ObjectMapper();

	protected static Set<ValidationMessage> checkRequestSchema(String jsonToValidate) throws IOException {
		return checkFileSchema(jsonToValidate, ekycVerifiedClaimsRequestResourceFile);
	}

	protected static Set<ValidationMessage> checkResponseSchema(String jsonToValidate) throws IOException {
		return checkFileSchema(jsonToValidate, ekycVerifiedClaimsResourceFile);
	}

	protected JsonElement getJsonElementFromResourceFile(String resourceFile) throws ConditionError {
		try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFile)) {
			return JsonParser.parseString(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw error("Failed to read JSON resource", e);
		} catch (JsonParseException e) {
			throw error("Failed to parse JSON resource", e);
		}
	}

	protected static Set<ValidationMessage> checkJsonNodeSchema(String jsonToValidate, JsonNode schemaJsonNode) throws IOException {
		JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaJsonNode), builder ->
			// This creates a mapping from $id which starts with https://bitbucket.org/openid/ekyc-ida/raw/master/schema/ to the retrieval URI resources:json-schemas/ekyc-ida/12/
			builder.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://bitbucket.org/openid/ekyc-ida/raw/master/schema/", "resource:json-schemas/ekyc-ida/12/"))
		);
		JsonSchema schema = factory.getSchema(schemaJsonNode);
		JsonNode node = mapper.readTree(jsonToValidate);
		Set<ValidationMessage> errors = schema.validate(node, executionContext -> {
			ExecutionConfig executionConfig = executionContext.getExecutionConfig();
			executionConfig.setDebugEnabled(true);
//			executionConfig.setAnnotationCollectionEnabled(true);
//			executionConfig.setAnnotationCollectionFilter(keyword -> true);
		});
		return errors;
	}

	protected static Set<ValidationMessage> checkStringSchema(String jsonToValidate, String schemaString) throws IOException {
		JsonNode schemaNode = mapper.readTree(schemaString);
		return checkJsonNodeSchema(jsonToValidate, schemaNode);
	}

	protected static Set<ValidationMessage> checkFileSchema(String jsonToValidate, String schemaFile) throws IOException {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaFile);

		JsonNode schemaNode = mapper.readTree(inputStream);
		return checkJsonNodeSchema(jsonToValidate, schemaNode);
	}

	protected void peformSchemaValidation(String dataElementName, JsonElement dataElement, String schemaElementName, JsonElement schemaElement) throws ConditionError {
		try {
			if(!schemaElement.isJsonObject()) {
				throw error("Schema element is not a JSON object", args(schemaElementName, schemaElement));
			}
			log("Validating data against schema", args(schemaElementName, schemaElement));
			Set<ValidationMessage> errors = checkStringSchema(dataElement.toString(), schemaElement.toString());
			if (!errors.isEmpty()) {
				JsonArray jsonErrors = new JsonArray();
				for (ValidationMessage error: errors) {
					jsonErrors.add(error.toString());
				}
				throw error("Failed to validate data against schema",
					args(schemaElementName, schemaElement,
						dataElementName, dataElement,
						"errors", jsonErrors));
			}
		} catch (IOException e) {
			throw error("Failed to parse JSON", e);
		}
	}

}
