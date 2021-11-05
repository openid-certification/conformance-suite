package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SuccessfulPaymentUpdate extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		logSuccess("Payment successfully updated within 5 minutes.");
		return env;
	}
}
