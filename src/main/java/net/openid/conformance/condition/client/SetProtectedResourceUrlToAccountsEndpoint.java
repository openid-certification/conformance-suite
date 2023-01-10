package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking.FAPIOBGetResourceEndpoint;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class SetProtectedResourceUrlToAccountsEndpoint extends AbstractCondition {

	private static final String ACCOUNTS_RESOURCE = "accounts";

	@Override
	@PreEnvironment(required = "resource")
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String resourceEndpoint = FAPIOBGetResourceEndpoint.getBaseResourceURL(env, FAPIOBGetResourceEndpoint.Endpoint.ACCOUNTS_RESOURCE);
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
