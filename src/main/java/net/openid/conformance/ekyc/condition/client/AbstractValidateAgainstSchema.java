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
		String schemaFile = "json-schemas/ekyc-ida/12/verified_claims_request.json";
		return checkSchema(jsonToValidate, schemaFile);
	}

	protected static Set<ValidationMessage> checkResponseSchema(String jsonToValidate) throws IOException {
		String schemaFile = "json-schemas/ekyc-ida/12/verified_claims.json";
		return checkSchema(jsonToValidate, schemaFile);
	}

	protected static Set<ValidationMessage> checkSchema(String jsonToValidate, String schemaFile) throws IOException {
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaFile);

		JsonNode schemaNode = mapper.readTree(inputStream);

		JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode), builder ->
			// This creates a mapping from $id which starts with https://bitbucket.org/openid/ekyc-ida/raw/master/schema/ to the retrieval URI resources:json-schemas/ekyc-ida/12/
			builder.schemaMappers(schemaMappers -> schemaMappers.mapPrefix("https://bitbucket.org/openid/ekyc-ida/raw/master/schema/", "resource:json-schemas/ekyc-ida/12/"))
		);
		JsonSchema schema = factory.getSchema(schemaNode);

		JsonNode node = mapper.readTree(jsonToValidate);

		Set<ValidationMessage> errors = schema.validate(node);

		return errors;
	}

}
