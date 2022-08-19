package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.HttpStatus;

public class EnsureTokenResponseWas401 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		Integer statusCode = env.getInteger("token_endpoint_response_http_status");
		if(statusCode == null) {
			throw error("token_endpoint_response_http_status was not found");
		}

		if(statusCode != HttpStatus.SC_UNAUTHORIZED) {
			throw error("Was expecting a 401 in the token response" , args("status", statusCode));
		}

		logSuccess("The status  code was 401, as expected", args("status", statusCode));
		return env;
	}

}
