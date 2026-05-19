package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class CreateAuthzenApiEndpointRequestParameter extends AbstractCondition {
	protected String requestParameterName;
	protected JsonElement requestParameter;
	protected String[] requiredProperties;
	protected String[] optionalProperties;

	public CreateAuthzenApiEndpointRequestParameter(String requestParameterName, JsonElement requestParameter) {
		this.requestParameterName = requestParameterName;
		this.requestParameter = requestParameter;
	}

	protected JsonElement createAuthzenApiEndpointRequestParameter(Environment env) {
		JsonObject authzenApiEndpointRequestParameter = new JsonObject();
		if (requestParameter != null && requestParameter.isJsonObject()) {
			JsonObject requestObject = requestParameter.getAsJsonObject();
			if (requiredProperties != null) {
				for (String requiredProperty : requiredProperties) {
					if (requestObject.has(requiredProperty)) {
						authzenApiEndpointRequestParameter.add(requiredProperty, requestObject.get(requiredProperty));
					} else {
						throw error(requestParameterName + " parameter is missing required property", args(requiredProperty, requestObject));
					}
				}
			}
			if (optionalProperties != null) {
				for (String optionProperty : optionalProperties) {
					if (requestObject.has(optionProperty)) {
						authzenApiEndpointRequestParameter.add(optionProperty, requestObject.get(optionProperty));
					}
				}
			}
		}
		return authzenApiEndpointRequestParameter;
	}
}
