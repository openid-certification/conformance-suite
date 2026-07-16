package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPICIBAEnsureRegistrationRequestUserCodeIsAbsentOrFalse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonElement userCode = env.getElementFromObject(
			"dynamic_registration_request", "backchannel_user_code_parameter");
		if (userCode == null) {
			logSuccess("Registration request omits CIBA user code support");
			return env;
		}
		if (!userCode.isJsonPrimitive() || !userCode.getAsJsonPrimitive().isBoolean()) {
			throw error("backchannel_user_code_parameter must be a boolean when supplied",
				args("backchannel_user_code_parameter", userCode));
		}
		if (OIDFJSON.getBoolean(userCode)) {
			throw error("Open Finance Brazil CIBA registration must not enable user code support",
				args("backchannel_user_code_parameter", true));
		}

		logSuccess("Registration request explicitly disables CIBA user code support");
		return env;
	}
}
