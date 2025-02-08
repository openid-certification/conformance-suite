package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFCheckStreamAudience extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonElement audience = env.getElementFromObject("ssf", "stream.aud");
		if (audience == null) {
			throw error("Missing audience, stream does not contain an 'aud' claim");
		}

		logSuccess("Found aud claim", args("aud", audience));
		return env;
	}

}
