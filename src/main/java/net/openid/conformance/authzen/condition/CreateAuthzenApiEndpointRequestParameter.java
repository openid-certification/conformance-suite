package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class CreateAuthzenApiEndpointRequestParameter extends AbstractCondition {
	protected String requestParameterName;
	protected JsonObject requestParameter;
	protected String[] requiredProperties; // assume to be string
	protected String[] optionalObjects; // assume to be objects
	public CreateAuthzenApiEndpointRequestParameter(String requestParameterName, JsonObject requestParameter) {
		this.requestParameterName = requestParameterName;
		this.requestParameter = requestParameter;
	}

	protected JsonObject createAuthzenApiEndpointRequestParameter(Environment env) {
		JsonObject authzenApiEndpointRequestParameter = new JsonObject();
		if(requestParameter != null) {
			if(requiredProperties != null) {
				for(String requiredProperty : requiredProperties) {
					if(requestParameter.has(requiredProperty)) {
						authzenApiEndpointRequestParameter.add(requiredProperty, requestParameter.get(requiredProperty));
					} else {
						throw error(requestParameterName + " parameter is missing required property", args(requiredProperty, requestParameter));
					}
				}
			}
			if(optionalObjects != null) {
				for(String optionalObject : optionalObjects) {
					if(requestParameter.has(optionalObject)) {
						authzenApiEndpointRequestParameter.add(optionalObject, requestParameter.get(optionalObject));
					}
				}
			}
		}
		return authzenApiEndpointRequestParameter;
	}

	protected JsonObject createAuthzenApiEndpointRequestWithAllParameters(Environment env) {
		if(requestParameter != null) {
			return requestParameter.deepCopy();
		}
		return new JsonObject();
	}

}
