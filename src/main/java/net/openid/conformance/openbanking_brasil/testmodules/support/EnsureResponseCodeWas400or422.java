package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas400or422 extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_full", "status");
		if (status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
			logSuccess(status + " response status returned");
		} else {
			throw error("Was expecting either a 400 or a 422 response, received " + status, args("status", status));
		}
		return env;
	}
}
