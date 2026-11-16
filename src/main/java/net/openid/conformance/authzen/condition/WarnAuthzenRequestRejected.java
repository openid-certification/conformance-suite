package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Flags a PDP that rejected a request it is expected to accept, for a module that also accepts the
 * rejection because the requirement to accept is not yet in the published specification text.
 * Intended to be called at WARNING; a status outside the module's acceptable set is a failure of
 * the status check.
 */
public class WarnAuthzenRequestRejected extends AbstractAuthzenResponseHeaderCondition {

	private final String expectation;

	/**
	 * @param expectation why the PDP is expected to accept the request, as shown in the message
	 */
	public WarnAuthzenRequestRejected(String expectation) {
		this.expectation = expectation;
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_response")
	public Environment evaluate(Environment env) {
		Integer status = responseStatus(env);
		if (status == null || status != 200) {
			throw error("The PDP rejected the request with HTTP " + status + ". " + expectation, args("status", status));
		}
		logSuccess("The PDP accepted the request", args("status", status));
		return env;
	}
}
