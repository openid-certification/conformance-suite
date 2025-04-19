package net.openid.conformance.vciid2issuer.util;

import com.google.gson.JsonObject;

public class JsonSchemaValidationInput {

	private String inputName;

	private String schemaResource;

	private JsonObject jsonObject;

	public JsonSchemaValidationInput(String inputName, String schemaResource, JsonObject jsonObject) {
		this.inputName = inputName;
		this.schemaResource = schemaResource;
		this.jsonObject = jsonObject;
	}

	public String getInputName() {
		return inputName;
	}

	public void setInputName(String inputName) {
		this.inputName = inputName;
	}

	public String getSchemaResource() {
		return schemaResource;
	}

	public void setSchemaResource(String schemaResource) {
		this.schemaResource = schemaResource;
	}

	public JsonObject getJsonObject() {
		return jsonObject;
	}

	public void setJsonObject(JsonObject jsonObject) {
		this.jsonObject = jsonObject;
	}
}
