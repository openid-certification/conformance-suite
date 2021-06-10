package net.openid.conformance.condition.as.par;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureAuthorizationRequestDoesNotContainRequestWhenUsingPAR extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_http_request_params"})
	public Environment evaluate(Environment env) {
		String requestObjectString = env.getString("authorization_endpoint_http_request_params", "request");
		if (!Strings.isNullOrEmpty(requestObjectString)) {
			throw error("Authorization request contains a request parameter when using PAR",
				args("request", requestObjectString));
		}
		logSuccess("Request does not contain a request parameter");
		return env;
	}

}
