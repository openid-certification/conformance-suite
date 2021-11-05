package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class FailedToUpdatePaymentInFiveMinutes extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		logFailure("Failed to update payment within 5 minutes");
		return env;
	}
}
