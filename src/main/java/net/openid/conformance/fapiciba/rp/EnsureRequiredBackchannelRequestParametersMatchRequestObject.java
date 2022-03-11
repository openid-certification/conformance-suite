package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * OIDCC 6.1 and 6.2 say:
 * So that the request is a valid OAuth 2.0 Authorization Request,
 * values for the response_type and client_id parameters MUST be included using the OAuth 2.0 request syntax,
 * since they are REQUIRED by OAuth 2.0.
 * The values for these parameters MUST match those in the Request Object, if present.
 *
 * scope does not need to match:
 * Even if a scope parameter is present in the Request Object value, a scope parameter
 * MUST always be passed using the OAuth 2.0 request syntax containing the openid scope
 * value to indicate to the underlying OAuth 2.0 logic that this is an OpenID Connect request.
 */
public class EnsureRequiredBackchannelRequestParametersMatchRequestObject extends AbstractCondition {

	public static final Set<String> parametersThatMustMatch = Set.of("response_type", "client_id");

	@Override
	@PreEnvironment(required = {"backchannel_endpoint_http_request_params", "backchannel_request_object"})
	public Environment evaluate(Environment env) {
		Map<String, Object> failureLogArgs = new HashMap<>();
		Map<String, Object> successLogArgs = new HashMap<>();

		for(String paramName : parametersThatMustMatch) {
			JsonElement valueFromHttpRequest = env.getElementFromObject("backchannel_endpoint_http_request_params", paramName);
			JsonElement valueFromRequestObject = env.getElementFromObject("backchannel_request_object", "claims." + paramName);
			if(valueFromHttpRequest==null) {
				//this is unlikely to happen as probably another condition will fail before this one
				//when one of these parameters are missing but just in case
				failureLogArgs.put(paramName, String.format("Required parameter '%s' was not found in http request parameters", paramName));
			} else {
				if(valueFromRequestObject!=null) {
					if(!valueFromHttpRequest.equals(valueFromRequestObject)) {
						Map<String, Object> paramValuesMapForLog = args("Value in http request", valueFromHttpRequest,
																		"Value in request object", valueFromRequestObject);
						failureLogArgs.put(paramName, paramValuesMapForLog);
					} else {
						successLogArgs.put(paramName, valueFromHttpRequest);
					}
				} else {
					successLogArgs.put(paramName, "Not found in request object");
				}
			}
		}

		if(failureLogArgs.isEmpty()) {
			logSuccess("Required http request parameters match request object claims", successLogArgs);
			return env;
		}
		throw error("Required http request parameters and request object claims must match", failureLogArgs);
	}

}
