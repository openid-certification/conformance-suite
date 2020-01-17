package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class DisallowMaxAgeEqualsZeroAndPromptNone extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {

		JsonElement maxAge = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.MAX_AGE);
		JsonElement maxAgeZero = new JsonPrimitive(0);
		String prompt = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.PROMPT);

		if (maxAgeZero.equals(maxAge) && "none".equals(prompt)) {
			throw error("Login required. Request contains max_age=0 and prompt=none parameters");
		} else {
			logSuccess("The client did not send max_age=0 and prompt=none parameters as expected");
			return env;
		}

	}

}
