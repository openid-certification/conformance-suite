package net.openid.conformance.vciid2issuer.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.networknt.schema.JsonNodePath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class JsonSchemaValidation {

	private final ObjectMapper mapper;

	private final Resource schemaResource;

	private Consumer<JsonSchemaFactory.Builder> schemaBuilderCustomizer;

	public JsonSchemaValidation(ObjectMapper mapper, Resource schemaResource) {
		this.mapper = mapper;
		this.schemaResource = schemaResource;
	}

	public JsonSchemaValidation(Resource schemaResource) {
		this(new ObjectMapper(), schemaResource);
	}

	public JsonSchemaValidation(String schemaResource) {
		this(new ClassPathResource(schemaResource));
	}

	public Consumer<JsonSchemaFactory.Builder> getSchemaBuilderCustomizer() {
		return schemaBuilderCustomizer;
	}

	public void setSchemaBuilderCustomizer(Consumer<JsonSchemaFactory.Builder> schemaBuilderCustomizer) {
		this.schemaBuilderCustomizer = schemaBuilderCustomizer;
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	public JsonSchemaValidationResult validate(JsonObject jsonObject) throws IOException {
		return validate(jsonObject.toString());
	}

	public JsonSchemaValidationResult validate(String jsonInput) throws IOException {

		JsonNode schemaNode = mapper.readTree(schemaResource.getInputStream());

		JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode), schemaBuilderCustomizer);
		JsonSchema schema = factory.getSchema(schemaNode);

		JsonNode inputNode = mapper.readTree(jsonInput);

		Set<ValidationMessage> errors = schema.validate(inputNode);

		return new JsonSchemaValidationResult(errors);
	}

	@SuppressWarnings("PMD.ForLoopVariableCount")
	public String toInstancePropertyPath(JsonNodePath path, String property) {
		var sb = new StringBuilder();
		for (int i = 0, cnt = path.getNameCount(); i < cnt; i++) {
			Object element = path.getElement(i);
			boolean isArrayIndex = element instanceof Integer;
			if (isArrayIndex) {
				if ('/' == sb.charAt(sb.length() - 1)) {
					sb.deleteCharAt(sb.length() - 1);
				}
				sb.append("[");
			}
			sb.append(path.getName(i));
			if (isArrayIndex) {
				sb.append("]");
			}
			if (cnt - 1 != i) {
				sb.append("/");
			}
		}
		if (property != null) {
			sb.append("/");
			sb.append(property);
		}
		return sb.toString();
	}

	public class JsonSchemaValidationResult {

		private final Set<ValidationMessage> validationMessages;

		public JsonSchemaValidationResult(Set<ValidationMessage> validationMessages) {
			this.validationMessages = validationMessages;
		}

		public boolean isValid() {
			return validationMessages.isEmpty();
		}

		public Set<ValidationMessage> getValidationMessages() {
			return validationMessages;
		}

		public List<JsonObject> getPropertyErrors() {
			List<JsonObject> propertyErrorsWithPaths = new ArrayList<>();
			for (ValidationMessage error : validationMessages) {
				JsonObject propertyError = new JsonObject();
				propertyError.addProperty("error", error.getError());
				if (error.getProperty() != null) {
					propertyError.addProperty("property", error.getProperty());
				}
				propertyError.addProperty("path", toInstancePropertyPath(error.getInstanceLocation(), error.getProperty()));
				propertyErrorsWithPaths.add(propertyError);
			}
			return propertyErrorsWithPaths;
		}
	}
}
