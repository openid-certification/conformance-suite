package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas200or406 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_full", "status");

		if(status != HttpStatus.OK.value() || status != HttpStatus.NOT_ACCEPTABLE.value()) {
			throw error("Was expecting a 200 or 406 response.");
		}

		logSuccess("Received the expected status code", args("Status code", status));

		return env;
	}

}
