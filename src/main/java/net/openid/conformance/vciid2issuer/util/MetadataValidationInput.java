package net.openid.conformance.vciid2issuer.util;

import com.google.gson.JsonObject;

public class MetadataValidationInput {

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
