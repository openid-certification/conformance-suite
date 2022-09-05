package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsurePAREndpointRequestDoesNotContainRequestUriParameter extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"par_endpoint_http_request"})
	public Environment evaluate(Environment env) {
		JsonObject parameters = env.getElementFromObject("par_endpoint_http_request", "body_form_params").getAsJsonObject();

		if (parameters.has("request_uri")) {
			throw error("PAR endpoint request contains a request_uri parameter");
		}
		logSuccess("PAR endpoint request does not contain a request_uri parameter");
		return env;
	}

}
