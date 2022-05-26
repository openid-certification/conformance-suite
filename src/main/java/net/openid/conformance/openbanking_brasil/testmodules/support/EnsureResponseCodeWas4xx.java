package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas4xx extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_full", "status");
		if(status >= 400 && status <= 499) {
			throw error("Was expecting a 4xx response");
		} else {
			logSuccess("4xx response status, as expected");
		}
		return env;
	}

}
