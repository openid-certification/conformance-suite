package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureBackchannelRequestObjectDoesNotContainUserCode extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_request_object")
	public Environment evaluate(Environment env) {
		JsonElement userCode = env.getElementFromObject("backchannel_request_object", "claims.user_code");
		if (userCode != null) {
			throw error("Backchannel request object contains user_code, which is not permitted for Open Finance Brasil CIBA",
				args("user_code", userCode));
		}

		logSuccess("Backchannel request object does not contain user_code");
		return env;
	}

}
