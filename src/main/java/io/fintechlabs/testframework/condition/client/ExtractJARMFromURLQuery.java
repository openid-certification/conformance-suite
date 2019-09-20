package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractExtractJWT;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractJARMFromURLQuery extends AbstractExtractJWT {

	@Override
	@PreEnvironment(required = "callback_query_params")
	@PostEnvironment(required = "jarm_response")
	public Environment evaluate(Environment env) {

		return extractJWT(env, "callback_query_params", "response", "jarm_response");

	}

}
