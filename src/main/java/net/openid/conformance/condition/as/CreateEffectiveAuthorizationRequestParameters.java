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
	@PreEnvironment(required = {"authorization_endpoint_http_request_params"})
	@PostEnvironment(required = {ENV_KEY})
	public Environment evaluate(Environment env) {

		JsonObject authzEndpointReqParams = env.getObject("authorization_endpoint_http_request_params");
		JsonObject effective = authzEndpointReqParams.deepCopy();

		//override request parameters if authorization_request_object exists
		if(env.containsObject("authorization_request_object")) {
			JsonObject requestObjectClaims = env.getElementFromObject("authorization_request_object", "claims").getAsJsonObject();

			for (String paramName : requestObjectClaims.keySet()) {
				//TODO do JsonNull values require special handling?
				effective.add(paramName, requestObjectClaims.get(paramName));
			}
		}
		//numeric values handling. (for now only max_age)
		for(String claimName : EnsureNumericRequestObjectClaimsAreNotNull.numericClaimNames) {
			if(effective.has(claimName)) {
				try {
					int valueAsInt = getParameterValueAsInt(claimName, effective.get(claimName));
					effective.addProperty(claimName, valueAsInt);
				} catch (ValueIsJsonNullException ex) {
					//value is json null. remove the entry from effective to prevent errors
					effective.remove(claimName);
					log(claimName + " has a json null value. Not including "+claimName+" in effective authorization endpoint request");
				}
			}
		}
		env.putObject(ENV_KEY, effective);
		logSuccess("Merged http request parameters with request object claims", args(ENV_KEY, effective));
		return env;
	}

	protected int getParameterValueAsInt(String parameterName, JsonElement jsonElement) throws ValueIsJsonNullException {
		if(jsonElement.isJsonPrimitive()) {
			//not using gson's getAsNumber to avoid sonarqube issues
			JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
			if(primitive.isString()) {
				String valueAsString = primitive.getAsString();
				try {
					int valueAsInt = Integer.parseInt(valueAsString);
					return valueAsInt;
				} catch (NumberFormatException formatException) {
					throw error(parameterName + " cannot be converted to a number", args(parameterName, jsonElement));
				}
			} else {

				try {
					return OIDFJSON.getNumber(jsonElement).intValue();
				} catch (Exception ex) {
					throw error(parameterName + " is not encoded as a number", args(parameterName, jsonElement));
				}
			}
		} else if (jsonElement.isJsonNull()) {
			throw new ValueIsJsonNullException(parameterName + " is a json null");
		} else {
			throw error("Unexpected "+parameterName+" parameter", args(parameterName, jsonElement));
		}
	}

	//Using a specific Exception class to make sure that we are not inadvertently catching some other exception
	@SuppressWarnings("serial")
	public class ValueIsJsonNullException extends Exception {
		public ValueIsJsonNullException(String msg) {
			super(msg);
		}
	}
}
