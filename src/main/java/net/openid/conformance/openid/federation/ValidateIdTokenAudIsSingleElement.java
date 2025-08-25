package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateIdTokenAudIsSingleElement extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "id_token" } )
	public Environment evaluate(Environment env) {

		JsonElement aud = env.getElementFromObject("id_token", "claims.aud");
		if (aud == null) {
			throw error("'aud' claim missing");
		}

		if (aud.isJsonArray() && aud.getAsJsonArray().size() != 1) {
			throw error("'aud' array should contain exactly one element", args("expected", 1, "actual", aud.getAsJsonArray().size()));
		}

		logSuccess("ID token aud claim passed additional validation checks");
		return env;

	}

}
