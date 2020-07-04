package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateRequestObjectAud extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_request_object", "client"})
	public Environment evaluate(Environment env) {
		String issuer = env.getString("server", "issuer"); // to validate the audience
		JsonElement aud = env.getElementFromObject("authorization_request_object", "claims.aud");
		if (aud == null) {
			throw error("Missing audience, request object does not contain an 'aud' claim");
		}

		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(issuer))) {
				throw error("aud claim values does not include the suite's issuer identifier",
							args("expected", issuer, "actual", aud));
			}
		} else {
			if (!issuer.equals(OIDFJSON.getString(aud))) {
				throw error("aud claim value does not match the suite's issuer identifier",
							args("expected", issuer, "actual", aud));
			}
		}
		logSuccess("aud claim matches the suite's issuer identifier", args("aud", aud));
		return env;
	}

}
