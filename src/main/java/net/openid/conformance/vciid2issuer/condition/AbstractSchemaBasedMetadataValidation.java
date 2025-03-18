package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vciid2issuer.util.JsonSchemaValidation;
import net.openid.conformance.vciid2issuer.util.JsonSchemaValidation.JsonSchemaValidationResult;

import java.io.IOException;

public abstract class AbstractSchemaBasedMetadataValidation extends AbstractCondition {

	protected abstract MetadataValidationInput createMetadataValidationInput(Environment env);

	@Override
	public Environment evaluate(Environment env) {

		MetadataValidationInput input = createMetadataValidationInput(env);

		JsonObject metadata = input.getMetadata();
		if (metadata == null) {
			throw error(String.format("%s metadata object not found", input.getMetadataName()));
		}

		JsonSchemaValidation jsonSchemaValidation = createJsonSchemaValidation(input);
		try {
			JsonSchemaValidationResult validationResult = jsonSchemaValidation.validate(metadata);
			if (!validationResult.isValid()) {
				onValidationFailure(validationResult, input);
			}
			onValidationSuccess(input);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return env;
	}

	protected void onValidationSuccess(MetadataValidationInput input) {
		logSuccess(String.format("%s metadata is valid", input.getMetadataName()), args("metadata", input.getMetadata()));
	}

	protected void onValidationFailure(JsonSchemaValidationResult validationResult, MetadataValidationInput input) {
		throw error(String.format("Found invalid entries in %s metadata", input.getMetadataName()), args("invalid_entries", validationResult.getPropertyErrors(), "metadata", input.getMetadata()));
	}

	protected JsonSchemaValidation createJsonSchemaValidation(MetadataValidationInput input) {
		return new JsonSchemaValidation(input.getSchemaResource());
	}

	public static class MetadataValidationInput {

		private String metadataName;

		private String schemaResource;

		private JsonObject metadata;

		public MetadataValidationInput(String metadataName, String schemaResource, JsonObject metadata) {
			this.metadataName = metadataName;
			this.schemaResource = schemaResource;
			this.metadata = metadata;
		}

		public String getMetadataName() {
			return metadataName;
		}

		public void setMetadataName(String metadataName) {
			this.metadataName = metadataName;
		}

		public String getSchemaResource() {
			return schemaResource;
		}

		public void setSchemaResource(String schemaResource) {
			this.schemaResource = schemaResource;
		}

		public JsonObject getMetadata() {
			return metadata;
		}

		public void setMetadata(JsonObject metadata) {
			this.metadata = metadata;
		}
	}
}
