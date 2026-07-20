package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPIBrazilCibaCheckUserCodeParameterNotSupported extends AbstractCondition {

	private static final String METADATA_KEY = "backchannel_user_code_parameter_supported";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement userCodeSupported = env.getElementFromObject("server", METADATA_KEY);

		if (userCodeSupported == null) {
			logSuccess(METADATA_KEY + " is absent and therefore defaults to false");
			return env;
		}

		if (!userCodeSupported.isJsonPrimitive()
			|| !userCodeSupported.getAsJsonPrimitive().isBoolean()
			|| OIDFJSON.getBoolean(userCodeSupported)) {
			throw error(METADATA_KEY + " must be absent or false",
				args("discovery_metadata_key", METADATA_KEY, "expected", false, "actual", userCodeSupported));
		}

		logSuccess(METADATA_KEY + " is false", args("actual", userCodeSupported));
		return env;
	}
}
