package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ThrowWarningFor406 extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_status");
		if(status == 406) {
			throw error("Response was 406");
		}
		return env;
	}
}
