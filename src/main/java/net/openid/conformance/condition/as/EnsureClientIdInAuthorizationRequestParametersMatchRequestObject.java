package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientIdInAuthorizationRequestParametersMatchRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params", "authorization_request_object"})
	public Environment evaluate(Environment env) {
		String requestParam = env.getString("authorization_endpoint_http_request_params", "client_id");
		String requestObjectValue = env.getString("authorization_request_object", "claims.client_id");
		if(requestParam==null) {
			throw error("client_id not found in http request parameters");
		}
		if(!requestParam.equals(requestObjectValue)) {
			throw error("client_id in http request parameters does not match client_id in request object",
				args("http_request_value", requestParam, "request_object_value", requestObjectValue));
		}
		logSuccess("client_id http request parameter value matches client_id in request object");
		return env;
	}

}
