package net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

public class UnavailableResourcesApiPollingTimeout extends AbstractJsonAssertingCondition {

	@Override
	@PostEnvironment(strings = "warning_message")
	public Environment evaluate(Environment env) {
		env.putString("warning_message", "Polling has ended with no resource on UNAVAILABLE/TEMPORARILY_UNAVAILABLE state, failing");
		throw error("Polling has ended with no resource on UNAVAILABLE/TEMPORARILY_UNAVAILABLE state, failing");
	}
}
