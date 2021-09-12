package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas200 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_status");
		if(status != HttpStatus.OK.value()) {
			throw error("Was expecting a 200 response");
		} else {
			logSuccess("200 response status, as expected");
		}
		return env;
	}

}
