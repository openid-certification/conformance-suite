package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class AddRedirectUriQuerySuffix extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String redirectUriSuffix = "?dummy1=lorem&dummy2=ipsum";

		env.putString("redirect_uri_suffix", redirectUriSuffix);

		logSuccess("Created redirect URI query suffix", args("redirect_uri_suffix", redirectUriSuffix));

		return env;
	}

}
