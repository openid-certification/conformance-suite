package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckAudInBindingJwtDcApi extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "client"})
	public Environment evaluate(Environment env) {
		String expected = "origin:"+env.getString("origin");
		JsonElement aud = env.getElementFromObject("sdjwt", "binding.claims.aud");
		if (aud == null) {
			throw error("'aud' claim missing");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(expected))) {
				throw error("'aud' array does not contain our origin", args("expected", expected, "actual", aud));
			}
		} else {
			if (!expected.equals(OIDFJSON.getString(aud))) {
				throw error("'aud' is not our origin", args("expected", expected, "actual", aud));
			}
		}

		logSuccess("aud in the binding jwt is the origin prefixed with 'origin:'");

		return env;
	}
}
