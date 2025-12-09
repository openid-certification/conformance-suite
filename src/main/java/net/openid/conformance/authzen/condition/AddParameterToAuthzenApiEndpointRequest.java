package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AddParameterToAuthzenApiEndpointRequest extends AbstractCondition {
	protected String parameterName;
	protected String parameterEnvObjectName;

	public AddParameterToAuthzenApiEndpointRequest(String parameterName, String parameterEnvObjectName) {
		this.parameterName = parameterName;
		this.parameterEnvObjectName = parameterEnvObjectName;
	}

	public Environment addParameterToAuthzenApiEndpointRequest(Environment env) {
		JsonObject paramEnvObject = env.getObject(parameterEnvObjectName);
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		if(!paramEnvObject.isEmpty()) {
			request.add(parameterName, paramEnvObject);
		} else {
			if(request.has(parameterName)) {
				request.remove(parameterName);
			}
		}
		logSuccess("Added parameter to authzen API request", args(parameterName, paramEnvObject));
		return env;
	}
}
