package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMaxAgeEqualsZeroAndPromptNone extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {

		JsonElement maxAge = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.MAX_AGE);
		JsonElement maxAgeZero = new JsonPrimitive(0);
		String prompt = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.PROMPT);

		if (maxAgeZero.equals(maxAge) && "none".equals(prompt)) {
			logSuccess("The client sent max_age=0 and prompt=none as expected");
			return env;
		} else {
			throw error("Invalid parameters. This test requires max_age=0 and prompt=none parameters",
						args("max_age", maxAge, "prompt", prompt));
		}

	}

}
