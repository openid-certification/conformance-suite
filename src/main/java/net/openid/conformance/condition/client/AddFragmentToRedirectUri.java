package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.DefaultUriBuilderFactory;

public class AddFragmentToRedirectUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "redirect_uri")
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString("redirect_uri");

		String redirectUriWithFragment =
				new DefaultUriBuilderFactory()
						.uriString(redirectUri)
						.fragment("foobar")
						.build()
						.toString();

		env.putString("redirect_uri", redirectUriWithFragment);

		log("Updated redirect_uri", args("redirect_uri", redirectUriWithFragment));

		return env;
	}

}
