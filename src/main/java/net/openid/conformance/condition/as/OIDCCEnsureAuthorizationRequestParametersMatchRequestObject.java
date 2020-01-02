package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * OIDCC 6.1 and 6.2 say:
 * So that the request is a valid OAuth 2.0 Authorization Request,
 * values for the response_type and client_id parameters MUST be included using the OAuth 2.0 request syntax,
 * since they are REQUIRED by OAuth 2.0.
 * The values for these parameters MUST match those in the Request Object, if present.
 *
 * TODO should scope also match?
 * Even if a scope parameter is present in the Request Object value, a scope parameter
 * MUST always be passed using the OAuth 2.0 request syntax containing the openid scope
 * value to indicate to the underlying OAuth 2.0 logic that this is an OpenID Connect request.
 */
public class OIDCCEnsureAuthorizationRequestParametersMatchRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params", "authorization_request_object"})
	public Environment evaluate(Environment env) {
		String responseTypeFromHttp = env.getString("authorization_endpoint_http_request_params", "response_type");
		String clientIdFromHttp = env.getString("authorization_endpoint_http_request_params", "client_id");

		String responseTypeFromRequestObject = env.getString("authorization_request_object", "claims.response_type");
		String clientIdFromRequestObject = env.getString("authorization_request_object", "claims.client_id");

		Map<String, Object> argsForLog = new HashMap<>();

		if(!Strings.isNullOrEmpty(responseTypeFromRequestObject)) {
			if(!responseTypeFromRequestObject.equals(responseTypeFromHttp)) {
				argsForLog.put("response_type_http_parameter", responseTypeFromHttp);
				argsForLog.put("response_type_request_object_claim", responseTypeFromRequestObject);
			}
		}
		if(!Strings.isNullOrEmpty(clientIdFromRequestObject)) {
			if(!clientIdFromRequestObject.equals(clientIdFromHttp)) {
				argsForLog.put("client_id_http_parameter", clientIdFromHttp);
				argsForLog.put("client_id_request_object_claim", clientIdFromRequestObject);
			}
		}

		if(argsForLog.isEmpty()) {
			logSuccess("Http request parameters and request object claims match",
						args("response_type", responseTypeFromRequestObject,
							"client_id", clientIdFromRequestObject));
			return env;
		}
		throw error("Http request parameters and request object claims must match", argsForLog);
	}

}
