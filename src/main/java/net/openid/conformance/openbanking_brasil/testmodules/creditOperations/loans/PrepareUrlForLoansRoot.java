package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForLoansRoot extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		setApi("loans");
		setEndpoint("/contracts");

		return super.evaluate(env);
	}
}
