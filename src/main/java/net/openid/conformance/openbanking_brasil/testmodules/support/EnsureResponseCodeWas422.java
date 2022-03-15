package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas422 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full" )
	public Environment evaluate(Environment env) {

		int status = env.getInteger("resource_endpoint_response_full", "status");

		if(status != HttpStatus.UNPROCESSABLE_ENTITY.value()) {
			throw error("Was expecting a 422 response");
		} else {
			logSuccess("422 response status, as expected");
		}
		return env;
	}

}
