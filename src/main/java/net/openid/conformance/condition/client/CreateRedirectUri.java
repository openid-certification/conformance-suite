package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Creates a callback URL based on the base_url environment value
 */
public class CreateRedirectUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "redirect_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		String suffix = env.getString("redirect_uri_suffix");

		if (!Strings.isNullOrEmpty(suffix)) {
			log("Appending suffix to redirect URI", args("suffix", suffix));
		} else {
			suffix = "";
		}

		// calculate the redirect URI based on our given base URL
		String redirectUri = baseUrl + "/callback" + suffix;
		env.putString("redirect_uri", redirectUri);

		logSuccess("Created redirect URI",
			args("redirect_uri", redirectUri));

		return env;
	}

}
