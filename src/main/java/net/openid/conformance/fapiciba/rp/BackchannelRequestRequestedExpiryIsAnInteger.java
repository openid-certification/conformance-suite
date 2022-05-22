package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelRequestRequestedExpiryIsAnInteger extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {

		Integer requestedExpiry = env.getInteger("backchannel_request_object", "claims.requested_expiry");

		if(requestedExpiry == null) {
			logSuccess("Backchannel authentication request does not contain optional parameter 'requested_expiry'");
			return env;
		} else {
			if(requestedExpiry <= 0) {
				throw error("The 'requested_expiry' must be a positive integer when present.");
			}
		}

		logSuccess("Backchannel authentication request contains valid parameter 'requested_expiry'");
		return env;
	}
}
