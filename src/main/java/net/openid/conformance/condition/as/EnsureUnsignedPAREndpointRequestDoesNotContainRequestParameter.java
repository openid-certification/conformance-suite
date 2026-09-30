package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureUnsignedPAREndpointRequestDoesNotContainRequestParameter extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"par_endpoint_http_request"})
	public Environment evaluate(Environment env) {
		if (env.getElementFromObject("par_endpoint_http_request", "body_form_params.request") != null) {
			throw error("PAR endpoint request contains a 'request' parameter (a request object), but this test was started with the 'Request Method' variant set to 'unsigned'. "
				+ "Either send the authorization request parameters directly as form parameters, or create a new test plan with 'Request Method' set to 'signed_non_repudiation'.");
		}
		logSuccess("PAR endpoint request does not contain a request parameter");
		return env;
	}

}
