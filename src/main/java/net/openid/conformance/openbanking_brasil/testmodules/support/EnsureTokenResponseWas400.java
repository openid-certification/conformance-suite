package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.HttpStatus;

public class EnsureTokenResponseWas400 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		Integer statusCode = env.getInteger("token_endpoint_response_http_status");
		if(statusCode == null) {
			throw error("token_endpoint_response_http_status was not found");
		}

		if(statusCode != HttpStatus.SC_BAD_REQUEST) {
			throw error("Was expecting a 400 in the token response" , args("status", statusCode));
		}

		logSuccess("The status  code was 400, as expected", args("status", statusCode));
		return env;
	}

}
