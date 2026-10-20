package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Run when the tester selected no PEP authentication: fails, for the caller to report as a warning,
 * when the PDP answered the request although it carried no credentials (Section 11.2: "the PDP
 * SHOULD authenticate the calling PEP").
 */
public class WarnPDPAcceptsUnauthenticatedRequests extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer status = env.getInteger("authzen_api_endpoint_response", "status");
		if (status != null && status == 200) {
			throw error("The PDP answered a request that carried no credentials. Section 11.2 says the PDP SHOULD "
				+ "authenticate the calling PEP, for example with mutual TLS, OAuth-based authentication or an API key",
				args("status", status));
		}
		logSuccess("The PDP did not answer the request that carried no credentials", args("status", status));
		return env;
	}
}
