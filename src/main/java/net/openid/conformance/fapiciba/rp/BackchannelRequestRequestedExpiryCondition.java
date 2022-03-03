package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelRequestRequestedExpiryCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {

		String requestedExpiryString = env.getString("backchannel_request_object", "claims.requested_expiry");

		if(requestedExpiryString == null) {
			logSuccess("Backchannel authentication request does not contain optional parameter 'requested_expiry'");
			return env;
		} else {
			try {
				Integer requestedExpiryValue = Integer.parseInt(requestedExpiryString);
				if(requestedExpiryValue <= 0) {
					throw error("The 'requested_expiry' must be a positive integer when present.");
				}
			} catch (NumberFormatException nfe) {
				throw error("The 'requested_expiry' must be a positive integer when present.");
			}
		}

		logSuccess("Backchannel authentication request contains valid parameter 'requested_expiry'");
		return env;
	}
}
