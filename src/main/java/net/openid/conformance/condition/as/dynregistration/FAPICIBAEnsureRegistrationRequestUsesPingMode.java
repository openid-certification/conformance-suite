package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPICIBAEnsureRegistrationRequestUsesPingMode extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonElement mode = env.getElementFromObject(
			"dynamic_registration_request", "backchannel_token_delivery_mode");
		if (mode == null || !mode.isJsonPrimitive() || !mode.getAsJsonPrimitive().isString()) {
			throw error("backchannel_token_delivery_mode must be the string ping",
				args("backchannel_token_delivery_mode", mode));
		}

		String modeValue = OIDFJSON.getString(mode);
		if (!"ping".equals(modeValue)) {
			throw error("Open Finance Brazil CIBA registration must use ping mode",
				args("backchannel_token_delivery_mode", modeValue, "required", "ping"));
		}

		logSuccess("Registration request uses CIBA ping mode");
		return env;
	}
}
