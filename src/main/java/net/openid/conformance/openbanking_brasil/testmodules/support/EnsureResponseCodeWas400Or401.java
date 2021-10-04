package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas400Or401 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_status");
		if(status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.UNAUTHORIZED.value()) {
			logSuccess("Valid response status returned");
		} else {
			throw error("Was expecting either a 400 or a 401 response");
		}
		return env;
	}

}
