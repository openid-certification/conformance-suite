package net.openid.conformance.util.validation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.google.gson.JsonObject;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.path.NodePath;
import net.openid.conformance.support.networknt.SpecificationVersionDetector;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.function.Consumer;

public class JsonSchemaValidation {

	private final ObjectMapper mapper;

	private final Resource schemaResource;

	private Consumer<SchemaRegistry.Builder> schemaBuilderCustomizer;

	public JsonSchemaValidation(ObjectMapper mapper, Resource schemaResource) {
		this.mapper = mapper;
		this.schemaResource = schemaResource;
	}

	public JsonSchemaValidation(Resource schemaResource) {
		this(new JsonMapper(), schemaResource);
	}

	public JsonSchemaValidation(String schemaResource) {
		this(new ClassPathResource(schemaResource));
	}

	public Consumer<SchemaRegistry.Builder> getSchemaBuilderCustomizer() {
		return schemaBuilderCustomizer;
	}

	public void setSchemaBuilderCustomizer(Consumer<SchemaRegistry.Builder> schemaBuilderCustomizer) {
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

		SpecificationVersion specVersion = SpecificationVersionDetector.detect(schemaNode);
		SchemaRegistry registry = schemaBuilderCustomizer == null
			? SchemaRegistry.withDefaultDialect(specVersion)
			: SchemaRegistry.withDefaultDialect(specVersion, schemaBuilderCustomizer);
		Schema schema = registry.getSchema(schemaNode);

		JsonNode inputNode = mapper.readTree(jsonInput);

		var errors = schema.validate(inputNode);

		return new JsonSchemaValidationResult(errors);
	}

	/**
	 * Returns the json path to the actual problem instance
	 * @param path
	 * @param property
	 * @return
	 */
	public static String toInstancePropertyPath(NodePath path, String property) {

		String propertyPath = path.toString();
		if (property != null) {
			propertyPath += "." + property;
		}

		return propertyPath;
	}

}
