package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PollPaymentAcceptedResultCheck extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		if (env.getBoolean("payment_accepted")) {
			logSuccess("Payment has successfully been accepted");
		} else {
			logFailure("Payment was not accepted in time");
		}
		return env;
	}
}
