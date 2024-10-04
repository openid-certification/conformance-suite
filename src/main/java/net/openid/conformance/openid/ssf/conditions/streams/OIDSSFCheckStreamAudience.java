package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFCheckStreamAudience extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf", "config"})
	public Environment evaluate(Environment env) {

		String expectedAud = env.getString("config", "ssf.stream.audience");
		JsonElement givenAud = env.getElementFromObject("ssf", "stream.aud");
		if (givenAud == null) {
			throw error("Missing audience, stream does not contain an 'aud' claim");
		}

		if (givenAud.isJsonArray() && !givenAud.getAsJsonArray().contains(new JsonPrimitive(expectedAud))) {
			throw error("aud claim values does not include the expected audience",
				args("expected", expectedAud, "actual", givenAud.getAsJsonArray()));
		}

		if (givenAud.getAsJsonPrimitive().isString() && !expectedAud.equals(OIDFJSON.getString(givenAud))) {
			throw error("aud claim value does not include the expected audience",
				args("expected", expectedAud, "actual", OIDFJSON.getString(givenAud)));
		}

		logSuccess("aud claim matches the expected audience", args("aud", givenAud));
		return env;
	}

}
