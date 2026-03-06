package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelRequestRequestedExpiryIsIgnoredForBrazil extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {

		JsonElement requestedExpiryClaim = env.getElementFromObject("backchannel_request_object", "claims.requested_expiry");
		env.removeObject("requested_expiry");

		if (requestedExpiryClaim == null) {
			logSuccess("Backchannel authentication request does not contain optional parameter 'requested_expiry'");
		} else {
			logSuccess("Ignoring 'requested_expiry' in backchannel authentication request for Brazil profile", args("requested_expiry", requestedExpiryClaim));
		}

		return env;
	}
}
