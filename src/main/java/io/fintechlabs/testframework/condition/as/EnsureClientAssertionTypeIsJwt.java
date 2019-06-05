package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureClientAssertionTypeIsJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String assertionType = env.getString("token_endpoint_request", "params.client_assertion_type");

		String expected = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

		if (expected.equals(assertionType)) {
			logSuccess("Found JWT assertion type", args("assertion type", expected));
			return env;
		} else if(assertionType == null){
			throw error("client_assertion_type missing from request parameters", args("expected", expected, "actual", null));
		} else  {
			throw error("client_assertion_type does not match JWT", args("expected", expected, "actual", assertionType));
		}
	}
}
