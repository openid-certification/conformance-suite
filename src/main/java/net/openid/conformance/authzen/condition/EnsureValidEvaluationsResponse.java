package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureValidEvaluationsResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_evaluations_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject evaluationsResponse = env.getObject("authzen_evaluations_endpoint_response");
		JsonElement evaluationsElem = evaluationsResponse.get("evaluations");
		if (null == evaluationsElem) {
			// TODO Check decision element if evaluations is not available
			throw error("No evaluations element in API response", args("authzen_evaluations_endpoint_response", evaluationsResponse));
		}
		if(!evaluationsElem.isJsonArray()) {
			throw error("Evaluations object in API response is not an array", args("authzen_evaluations_endpoint_response", evaluationsResponse));
		}
		for(JsonElement elem : evaluationsElem.getAsJsonArray()) {
			if(!elem.isJsonObject()) {
				throw error("An element in the evaluations array is not an object", args("Element", elem));
			}
			JsonObject decisionObj = elem.getAsJsonObject();
			if(!decisionObj.has("decision")) {
				throw error("A Decision element in the evaluations response does not contain decision value", args("Evaluations decision object", decisionObj));
			}
			JsonElement decisionElement = decisionObj.get("decision");
			if(!decisionElement.isJsonPrimitive()) {
				throw error("A Decision element in the evaluations response is not a primitive", args("decision", decisionObj));
			}
			JsonPrimitive jsonPrimitive = decisionElement.getAsJsonPrimitive();
			if(!jsonPrimitive.isBoolean()) {
				throw error("A Decision element in the evaluations response is not a boolean value", args("decision", decisionObj));
			}
		}
		logSuccess("Evaluations response is valid");
		return env;

	}

}
