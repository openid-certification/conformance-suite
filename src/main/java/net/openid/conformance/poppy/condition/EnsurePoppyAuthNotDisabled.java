package net.openid.conformance.poppy.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsurePoppyAuthNotDisabled extends AbstractCondition {

	@Override
	@PreEnvironment(required = "poppy_json")
	public Environment evaluate(Environment env) {
		JsonElement disabled = env.getElementFromObject("poppy_json", "auth.disabled");
		if (disabled != null && disabled.isJsonPrimitive() && disabled.getAsJsonPrimitive().isBoolean() && OIDFJSON.getBoolean(disabled)) {
			throw error("poppy.json sets 'auth.disabled' to true. This test exercises Direct Sign-In, which section 3.2 forbids a Personal Agent to start while 'auth.disabled' is set; the Company must enable sign-in for Personal Agents before running this test.");
		}
		logSuccess("The Company does not disable sign-in for Personal Agents");
		return env;
	}
}
