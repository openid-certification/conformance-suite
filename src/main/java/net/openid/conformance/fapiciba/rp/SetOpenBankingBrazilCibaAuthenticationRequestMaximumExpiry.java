package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.fapiciba.OpenBankingBrazilCibaMaximumExpiry;
import net.openid.conformance.testmodule.Environment;

public class SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry extends AbstractCondition {

	public static final String ENVIRONMENT_KEY = "backchannel_authentication_request_maximum_expiry";

	@Override
	@PostEnvironment(integers = ENVIRONMENT_KEY)
	public Environment evaluate(Environment env) {
		OpenBankingBrazilCibaMaximumExpiry.MaximumExpiry maximumExpiry;
		try {
			maximumExpiry = OpenBankingBrazilCibaMaximumExpiry.resolve(env);
		} catch (IllegalArgumentException e) {
			throw error(e.getMessage());
		}

		env.putInteger(ENVIRONMENT_KEY, maximumExpiry.seconds());
		logSuccess("Set Open Finance Brasil data consent authentication request maximum expiry",
			args("maximum_expiry_seconds", maximumExpiry.seconds(),
				"maximum_expiry_explicitly_configured", maximumExpiry.explicitlyConfigured()));
		return env;
	}
}
