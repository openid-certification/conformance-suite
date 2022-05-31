package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelEnsureClientAssertionTypeIsJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	public Environment evaluate(Environment env) {

		String assertionType = env.getString("backchannel_endpoint_http_request", "body_form_params.client_assertion_type");

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
