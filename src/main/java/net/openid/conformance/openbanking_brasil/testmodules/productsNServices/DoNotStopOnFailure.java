package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class DoNotStopOnFailure extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.mapKey("doNotStopOnFailure", "true");

		return env;
	}
}
