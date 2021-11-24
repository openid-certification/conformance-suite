package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class Ensure422ResponseCodeWasDETALHE_PGTO_INVALIDO extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		log(env.toString());
		return env;
	}
}
