package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class AddRedirectUriQuerySuffix extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String redirectUriSuffix = "?dummy1=lorem&dummy2=ipsum";

		env.putString("redirect_uri_suffix", redirectUriSuffix);

		logSuccess("Created redirect URI query suffix to test that query sections in the registered redirect url are handled correctly. The redirect url, including this suffix, must be registered for the client as per http://openid.net/certification/fapi_op_testing/", args("redirect_uri_suffix", redirectUriSuffix));

		return env;
	}

}
