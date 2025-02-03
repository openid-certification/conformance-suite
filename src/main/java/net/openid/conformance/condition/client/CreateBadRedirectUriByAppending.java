package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateBadRedirectUriByAppending extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "redirect_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		// create a random redirect URI, by appending a random path, which shouldn't be registered with the server
		String badRedirectPath = RandomStringUtils.secure().nextAlphanumeric(10);
		String redirectUri = baseUrl + "/callback/" + badRedirectPath;
		env.putString("redirect_uri", redirectUri);
		env.putString("bad_redirect_path", badRedirectPath);

		logSuccess("Created a randomised (and hence unregistered) redirect URI",
			args("redirect_uri", redirectUri));

		return env;
	}

}
