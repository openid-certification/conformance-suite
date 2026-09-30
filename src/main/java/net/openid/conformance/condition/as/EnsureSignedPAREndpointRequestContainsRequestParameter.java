package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureSignedPAREndpointRequestContainsRequestParameter extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"par_endpoint_http_request"})
	public Environment evaluate(Environment env) {
		if (env.getElementFromObject("par_endpoint_http_request", "body_form_params.request") == null) {
			throw error("PAR endpoint request does not contain a 'request' parameter (a signed request object), but this test was started with the 'Request Method' variant set to 'signed_non_repudiation'. "
				+ "Either send the authorization request parameters in a signed request object, or, if the profile being tested permits it, create a new test plan with 'Request Method' set to 'unsigned'.",
				args("par_endpoint_request_parameters", env.getElementFromObject("par_endpoint_http_request", "body_form_params")));
		}
		logSuccess("PAR endpoint request contains a request parameter");
		return env;
	}

}
