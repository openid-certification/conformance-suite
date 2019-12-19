package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckRequiredAuthorizationParametersPresent extends AbstractCondition {
	//TODO review before use. this class was not used when it was refactored to use CreateEffectiveAuthorizationRequestParameters.
	@Override
	@PreEnvironment(required = {CreateEffectiveAuthorizationRequestParameters.ENV_KEY})
	public Environment evaluate(Environment env) {


		List<String> responses = new ArrayList<>();
		responses.add(env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE));
		responses.add(env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.CLIENT_ID));
		responses.add(env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.REDIRECT_URI));
		responses.add(env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.SCOPE));

		for (String singleResponse : responses) {
			if (Strings.isNullOrEmpty(singleResponse)) {
				throw error("Required parameter value(s) not present in the authorization endpoint request", args("Missing parameter", singleResponse));
			}
		}

		logSuccess("Required parameter values are found outside of the post body", args("parameters", responses));

		return env;
	}
}
