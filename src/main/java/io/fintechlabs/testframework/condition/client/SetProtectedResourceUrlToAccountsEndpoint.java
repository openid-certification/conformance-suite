package io.fintechlabs.testframework.condition.client;

import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.openbanking.FAPIOBGetResourceEndpoint;
import io.fintechlabs.testframework.openbanking.FAPIOBGetResourceEndpoint.Endpoint;
import io.fintechlabs.testframework.testmodule.Environment;

public class SetProtectedResourceUrlToAccountsEndpoint extends AbstractCondition {

	private static final String ACCOUNTS_RESOURCE = "accounts";

	@Override
	@PreEnvironment(required = "resource")
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String resourceEndpoint = FAPIOBGetResourceEndpoint.getBaseResourceURL(env, Endpoint.ACCOUNTS_RESOURCE);
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			throw error("Resource endpoint not found");
		}

		// Build the endpoint URL
		String resourceUrl = UriComponentsBuilder.fromUriString(resourceEndpoint)
			.path(ACCOUNTS_RESOURCE)
			.toUriString();

		env.putString("protected_resource_url", resourceUrl);

		logSuccess("Set protected resource URL", args("protected_resource_url", resourceUrl));

		return env;
	}

}
