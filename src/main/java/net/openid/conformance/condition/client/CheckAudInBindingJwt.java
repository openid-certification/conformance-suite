package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckAudInBindingJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "client"})
	public Environment evaluate(Environment env) {
		String clientId = env.getString("client", "client_id"); // to check the audience
		JsonElement aud = env.getElementFromObject("sdjwt", "binding.claims.aud");
		if (aud == null) {
			throw error("'aud' claim missing");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(clientId))) {
				throw error("'aud' array does not contain our client id", args("expected", clientId, "actual", aud));
			}
		} else {
			if (!clientId.equals(OIDFJSON.getString(aud))) {
				throw error("'aud' is not our client id", args("expected", clientId, "actual", aud));
			}
		}

		logSuccess("aud in the binding jwt is the client_id");

		return env;
	}
}
