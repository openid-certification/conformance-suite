package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.fapiciba.OpenBankingBrazilCibaProfileConstants;
import net.openid.conformance.testmodule.Environment;

public class SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry extends AbstractCondition {

	public static final String ENVIRONMENT_KEY = "backchannel_authentication_request_maximum_expiry";

	@Override
	@PostEnvironment(integers = ENVIRONMENT_KEY)
	public Environment evaluate(Environment env) {
		env.putInteger(ENVIRONMENT_KEY,
			OpenBankingBrazilCibaProfileConstants.DEFAULT_AUTHENTICATION_REQUEST_MAXIMUM_EXPIRY_SECONDS);
		logSuccess("Set Open Finance Brasil data consent authentication request maximum expiry",
			args("maximum_expiry_seconds",
				OpenBankingBrazilCibaProfileConstants.DEFAULT_AUTHENTICATION_REQUEST_MAXIMUM_EXPIRY_SECONDS));
		return env;
	}
}
