package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;

public class CheckItemCountHasMin3 extends AbstractCheckItemCount {

	@Override
	public Environment evaluate(Environment env) {
		return super.checkItemCount(env, 3);
	}
}
