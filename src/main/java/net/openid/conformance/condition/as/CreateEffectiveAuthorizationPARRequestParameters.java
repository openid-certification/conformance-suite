package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Merges http request parameters and unsigned PAR and request object parameters
 * and creates effective_authorization_endpoint_request environment entry
 */
public class CreateEffectiveAuthorizationPARRequestParameters extends CreateEffectiveAuthorizationRequestParameters {

	//WARNING "authorization_request_object" is also used but it's not required
	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params", "par_endpoint_http_request_params"})
	@PostEnvironment(required = {ENV_KEY})
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected void customizeEffectiveAuthorizationRequestParams(Environment env, JsonObject effective) {
		JsonObject parEndpointReqParams = env.getObject("par_endpoint_http_request_params");
		effective.remove("client_assertion");
		effective.remove("client_assertion_type");
		effective.remove("client_secret");

		// overrride with unsigned PAR params
		for(String paramName : parEndpointReqParams.keySet()) {
			effective.add(paramName, parEndpointReqParams.get(paramName));
		}
	}
}
