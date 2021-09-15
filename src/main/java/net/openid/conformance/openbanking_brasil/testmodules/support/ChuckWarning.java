package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

public class ChuckWarning extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "warning_message")
	public Environment evaluate(Environment env) {
		String warningMessage = env.getString("warning_message");
		throw error(warningMessage);
	}
}
