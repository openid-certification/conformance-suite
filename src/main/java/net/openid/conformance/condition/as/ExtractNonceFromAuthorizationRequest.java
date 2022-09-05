package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractNonceFromAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	@PostEnvironment(strings = "nonce")
	public Environment evaluate(Environment env) {

		String nonce = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.NONCE);

		if (Strings.isNullOrEmpty(nonce)) {
			throw error("Couldn't find 'nonce' in authorization endpoint parameters");
		} else {
			env.putString("nonce", nonce);

			logSuccess("Extracted nonce", args("nonce", nonce));
			return env;
		}

	}

}
