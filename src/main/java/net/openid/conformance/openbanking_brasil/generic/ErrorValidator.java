package net.openid.conformance.openbanking_brasil.generic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;


public class ErrorValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertBodyExists(body);

		validateErrors(body);

		int status = environment.getInteger("resource_endpoint_response_status");
		validateStatus(body, status);

		return environment;
	}

	private void assertBodyExists(JsonObject body){
		if(body == null){
			throw error("No body existing for the resource_endpoint_response");
		} else if(body.keySet().isEmpty()) {
			throw error("No keys for resource_endpoint_response: " + body);
		} else {
			logSuccess("Response body found successfully");
		}
	}

	private void validateErrors(JsonObject body){
		if(body.get("errors") != null){
			if(body.get("errors").isJsonArray()){
				logSuccess("Error response is compliant with the spec");
			} else {
				logFailure(body);
				throw error("Error response is not compliant with the spec");
			}
		} else {
			logFailure(body);
			logFailure("Error needed but no errors found. Not spec compliant");
		}
	}

	private void validateStatus(JsonObject body, int status){
		if(body.get("errors").isJsonArray()){
			// This is disgusting, however getAsJsonArray is not allowed.
			JsonArray errors = (JsonArray) body.get("errors");
			errors.forEach(error -> {
				JsonObject obj = OIDFJSON.toObject(error);
				String code = OIDFJSON.getString(obj.get("code"));
				if(Integer.parseInt(code) == status){
					logSuccess("Error status code is matching");
				} else {
					logFailure("Error status code does not match");
				}
			});
		} else {
			throw error("Errors is not an array. This is not spec compliant");
		}



	}

}
