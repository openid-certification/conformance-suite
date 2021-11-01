package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas403or400 extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_status");
		if(status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.FORBIDDEN.value()) {
			logSuccess("Valid response status returned");
		} else {
			throw error("Was expecting either a 403 or a 401 response");
		}
		return env;
	}
}
