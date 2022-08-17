package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class VerifyResourcePollingSequenceStopCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_full", "status");

		env.putBoolean("200Ok_or_differentCode_found", false);
		if(status != HttpStatus.ACCEPTED.value()) {
			env.putBoolean("200Ok_or_differentCode_found", true);
			log("A status code different from 202 was found, stop polling.");
		} else {
			log("A status code different from 200 was found, keep polling");
		}

		return env;
	}
}
