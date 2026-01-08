package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetLoginHintToConsentId extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"consent_id"})
	public Environment evaluate(Environment env) {
		String consentId = env.getString("consent_id");
		env.putString("config", "client.hint_value", consentId);
		logSuccess("Set login_hint to the previously obtained consent_id", args("login_hint", consentId));

		return env;
	}
}
