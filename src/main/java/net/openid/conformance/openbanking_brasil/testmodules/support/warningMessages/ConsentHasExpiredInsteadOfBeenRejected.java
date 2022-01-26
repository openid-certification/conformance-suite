package net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

public class ConsentHasExpiredInsteadOfBeenRejected extends AbstractJsonAssertingCondition {

	@Override
	@PostEnvironment(strings = "warning_message")
	public Environment evaluate(Environment env) {
		log("Setting warning message");
		env.putString("warning_message", "To certifier: Check the consent actually expired instead of being manually rejected.");
		return env;
	}
}
