package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.DefaultUriBuilderFactory;

public class AddQueryToRedirectUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "redirect_uri")
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString("redirect_uri");

		String redirectUriWithQuery =
				new DefaultUriBuilderFactory()
						.uriString(redirectUri)
						.queryParam("bar", "foo")
						.build()
						.toString();

		env.putString("redirect_uri", redirectUriWithQuery);

		log("Updated redirect_uri", args("redirect_uri", redirectUriWithQuery));

		return env;
	}

}
