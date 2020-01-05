package net.openid.conformance.condition.as;

import java.util.Set;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractEnsureResponseType extends AbstractCondition {

	protected Environment ensureResponseTypeMatches(Environment env, String... types) {

		String responseTypeString = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.RESPONSE_TYPE);
		if (Strings.isNullOrEmpty(responseTypeString)) {
			throw error("Could not find response type in request");
		}

		String expectedString = String.join(" ", types);

		Set<String> responseType = Set.of(responseTypeString.split(" "));
		Set<String> expected = Set.of(types);

		if (!responseType.equals(expected)) {
			throw error("Response type is not expected value", args("expected", expectedString, "actual", responseTypeString));
		} else {
			logSuccess("Response type is expected value", args("expected", expectedString));
			return env;
		}

	}
}
