package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetConnectIdCibaLoginHintFromConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String loginHint = env.getString("config", "client.login_hint");
		if (Strings.isNullOrEmpty(loginHint)) {
			throw error("'Login hint' field is missing from the 'Client' section in the test configuration");
		}

		env.putString("config", "client.hint_type", "login_hint");
		env.putString("config", "client.hint_value", loginHint);

		logSuccess("Set ConnectID CIBA login_hint from the test configuration",
			args("login_hint", loginHint));

		return env;
	}
}
