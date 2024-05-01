package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckForInvalidCharsInState extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {

		List<String> invalidCharacters = new ArrayList<>();
		String state = env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.STATE);

		if (! Strings.isNullOrEmpty(state)) {
			// Ensure the state contains only URL safe characters.
			for (int i = 0; i < state.length(); i++) {
				String charAsString = String.valueOf(state.charAt(i));

				if (! charAsString.matches("[A-Za-z0-9\\-_\\.~]")) {
					if (! invalidCharacters.contains(charAsString)) {
						invalidCharacters.add(charAsString);
					}
				}
			}

			if (! invalidCharacters.isEmpty()) {
				throw error("Non URL safe characters found in state. This may introduce interoperability issues.",
					args("state", state, "invalid_chars", invalidCharacters));
			}
		}

		logSuccess("State is empty or contains only URL safe characters");
		return env;
	}
}
