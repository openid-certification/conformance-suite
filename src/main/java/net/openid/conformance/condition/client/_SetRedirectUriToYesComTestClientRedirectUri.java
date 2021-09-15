package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Creates a callback URL based on the base_url environment value
 */
public class _SetRedirectUriToYesComTestClientRedirectUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "redirect_uri")
	public Environment evaluate(Environment in) {
		in.putString("redirect_uri", "http://localhost:3000/yes/oidccb");

		logSuccess("Set redirect URI",
			args("redirect_uri", "http://localhost:3000/yes/oidccb"));

		return in;
	}

}
