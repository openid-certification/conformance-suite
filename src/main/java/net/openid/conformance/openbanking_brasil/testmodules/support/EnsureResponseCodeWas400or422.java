package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas400or422 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		int status = env.getInteger("resource_endpoint_response_status");
		if(status == HttpStatus.BAD_REQUEST.value()) {
			logSuccess("400 response status returned");
		} else if (status == HttpStatus.UNPROCESSABLE_ENTITY.value()){
			logSuccess("422 response status returned");
		} else {
			throw error("Was expecting either a 400 or a 422 response");
		}
		return env;
	}
}
