package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckTokenEndpointHttpStatusNot200 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		Integer httpStatus = env.getInteger("token_endpoint_response_http_status");

		if (httpStatus == null) {
			throw error("Http status can not be null.");
		}

		if (httpStatus == 200) {
			throw error("The token endpoint returned http status code 200, when it should return http status code 400 and an error.");
		}

		return env;
	}
}
