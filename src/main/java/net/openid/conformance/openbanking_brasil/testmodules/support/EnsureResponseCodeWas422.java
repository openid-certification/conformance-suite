package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;

public class EnsureResponseCodeWas422 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		try {
			String statusAsString = env.getString("resource_endpoint_response_status");
			if (statusAsString == null) {
				throw error("Was expecting a response status but none was present");
			}
		} catch (Environment.UnexpectedTypeException ignored){
			//this is to ignore
		}
		int status = env.getInteger("resource_endpoint_response_status");
		if(status != HttpStatus.UNPROCESSABLE_ENTITY.value()) {
			throw error("Was expecting a 422 response");
		} else {
			logSuccess("422 response status, as expected");
		}
		return env;
	}

}
