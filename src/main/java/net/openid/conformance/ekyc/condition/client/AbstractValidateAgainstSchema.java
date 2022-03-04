package net.openid.conformance.ekyc.condition.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import net.openid.conformance.condition.AbstractCondition;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public abstract class AbstractValidateAgainstSchema extends AbstractCondition {

	private static ObjectMapper mapper = new ObjectMapper();

	protected static Set<ValidationMessage> checkRequestSchema(String jsonToValidate) throws IOException {
		String schemaFile = "json-schemas/ekyc/verified_claims_request-12.json";
		return checkSchema(jsonToValidate, schemaFile);
	}

	protected static Set<ValidationMessage> checkResponseSchema(String jsonToValidate) throws IOException {
		String schemaFile = "json-schemas/ekyc/verified_claims-12.json";
		return checkSchema(jsonToValidate, schemaFile);
	}

	protected static Set<ValidationMessage> checkSchema(String jsonToValidate, String schemaFile) throws IOException {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaFile);

		JsonNode schemaNode = mapper.readTree(inputStream);

		JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode));
		JsonSchema schema = factory.getSchema(schemaNode);

		JsonNode node = mapper.readTree(jsonToValidate);

		Set<ValidationMessage> errors = schema.validate(node);

		return errors;
	}

}
