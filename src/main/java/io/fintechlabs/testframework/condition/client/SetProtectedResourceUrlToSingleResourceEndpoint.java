package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class SetProtectedResourceUrlToSingleResourceEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource")
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String resourceUrl = env.getString("resource", "resourceUrl");

		if(Strings.isNullOrEmpty(resourceUrl)){
			throw error("Missing Resource URL");
		}

		env.putString("protected_resource_url", resourceUrl);

		logSuccess("Set protected resource URL", args("protected_resource_url", resourceUrl));

		return env;
	}

}
