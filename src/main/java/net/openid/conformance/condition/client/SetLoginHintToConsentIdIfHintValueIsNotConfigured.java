package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetLoginHintToConsentIdIfHintValueIsNotConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"consent_id"}, required = {"config"})
	public Environment evaluate(Environment env) {
		String hintType = env.getString("config", "client.hint_type");
		String currentlyConfiguredHintValue = env.getString("config", "client.hint_value");
		if (Strings.isNullOrEmpty(currentlyConfiguredHintValue) && "login_hint".equals(hintType)) {
			env.putString("config", "client.hint_value", env.getString("consent_id"));
			logSuccess("Set login_hint to the previously obtained consent_id", args("hint_type", hintType, hintType, currentlyConfiguredHintValue));
		} else {
			logSuccess("Using configured hint_value", args("hint_type", hintType, hintType, currentlyConfiguredHintValue));
		}
		return env;
	}
}
