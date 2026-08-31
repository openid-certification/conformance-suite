package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureBackchannelRequestDoesNotContainRequestedExpiryForBrazil extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {
		JsonElement requestedExpiry = env.getElementFromObject("backchannel_request_object", "claims.requested_expiry");
		if (requestedExpiry != null) {
			throw error("Backchannel request must not contain requested_expiry for Open Finance Brasil CIBA",
				args("requested_expiry_present", true));
		}

		logSuccess("Backchannel request does not contain requested_expiry");
		return env;
	}
}
