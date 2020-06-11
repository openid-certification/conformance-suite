package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateInitiateLoginUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "initiate_login_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		// Note that this url isn't actually ever called currently (we only pass it in the DCR request), but for consistency allow it to be overridden
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}

		// calculate the redirect URI based on our given base URL
		String initiateLoginUri = baseUrl + "/initiate_login";
		env.putString("initiate_login_uri", initiateLoginUri);

		logSuccess("Created initiate_login URI",
			args("initiate_login_uri", initiateLoginUri));

		return env;
	}

}
