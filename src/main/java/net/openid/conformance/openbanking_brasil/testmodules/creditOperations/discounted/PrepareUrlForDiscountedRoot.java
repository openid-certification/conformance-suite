package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForDiscountedRoot  extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		setApi("invoice-financings");
		setEndpoint("/contracts");

		return super.evaluate(env);
	}
}
