package net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

public class TestTimedOut extends AbstractJsonAssertingCondition {

	@Override
	@PostEnvironment(strings = "warning_message")
	public Environment evaluate(Environment env) {
		env.putString("warning_message", "Participant took too long to reject request, failing");
		return env;
	}
}
