package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilMustTestUsingPayments extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		throw error("Brazil testing must be done using the payments API. Please specify payments in the scope and use the payments endpoints.");
	}

}
