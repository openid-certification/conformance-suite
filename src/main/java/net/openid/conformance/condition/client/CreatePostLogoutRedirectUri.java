package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreatePostLogoutRedirectUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "post_logout_redirect_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		String postLogoutRedirectUri = baseUrl + "/post_logout_redirect";
		env.putString("post_logout_redirect_uri", postLogoutRedirectUri);

		logSuccess("Created post_logout_redirect_uri URI",
			args("post_logout_redirect_uri", postLogoutRedirectUri));

		return env;
	}

}
