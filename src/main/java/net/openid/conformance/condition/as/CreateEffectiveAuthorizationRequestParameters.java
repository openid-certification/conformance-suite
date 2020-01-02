package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Merges http request parameters and request object parameters
 * and creates effective_authorization_endpoint_request environment entry
 */
public class CreateEffectiveAuthorizationRequestParameters extends AbstractCondition {

	public static final String ENV_KEY = "effective_authorization_endpoint_request";

	//always use these constants to get values. just in case the keys, JsonObject structure change etc
	public static final String MAX_AGE = "max_age";
	public static final String PROMPT = "prompt";
	public static final String STATE = "state";
	public static final String REDIRECT_URI = "redirect_uri";
	public static final String RESPONSE_TYPE = "response_type";
	public static final String CLIENT_ID = "client_id";
	public static final String SCOPE = "scope";
	public static final String NONCE = "nonce";

	//WARNING "authorization_request_object" is also used but it's not required
	@Override
	@PreEnvironment(strings = {"http_request_params_source"}, required = {"authorization_endpoint_http_request"})
	@PostEnvironment(required = {ENV_KEY})
	public Environment evaluate(Environment env) {
		String paramsSource = env.getString("http_request_params_source");

		JsonObject authzEndpointReqParams = env.getElementFromObject("authorization_endpoint_http_request", paramsSource).getAsJsonObject();
		JsonObject effective = authzEndpointReqParams.deepCopy();

		//override request parameters if authorization_request_object exists
		if(env.containsObject("authorization_request_object")) {
			JsonObject requestObjectClaims = env.getElementFromObject("authorization_request_object", "claims").getAsJsonObject();

			for (String paramName : requestObjectClaims.keySet()) {
				effective.add(paramName, requestObjectClaims.get(paramName));
			}
		}
		//max_age special case
		if(effective.has(MAX_AGE)) {
			//We can't use OIDFJSON here because MAX_AGE comes as a string from http request parameters
			int maxAgeAsInt = getParameterValueAsInt(MAX_AGE, effective.get(MAX_AGE));
			effective.addProperty(MAX_AGE, maxAgeAsInt);
		}
		env.putObject(ENV_KEY, effective);
		logSuccess("Merged http request parameters with request object claims", args(ENV_KEY, effective));
		return env;
	}

	protected int getParameterValueAsInt(String parameterName, JsonElement jsonElement) {
		if(jsonElement.isJsonPrimitive()) {
			//not using gson's getAsNumber to avoid sonarqube issues
			JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
			if(primitive.isString()) {
				String valueAsString = primitive.getAsString();
				int valueAsInt = Integer.parseInt(valueAsString);
				return valueAsInt;
			} else {
				return OIDFJSON.getNumber(jsonElement).intValue();
			}
		} else {
			throw error("Unexpected "+parameterName+" parameter", args(parameterName, jsonElement));
		}
	}
}
