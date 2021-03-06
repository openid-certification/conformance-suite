package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateFrontchannelLogoutUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "frontchannel_logout_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}

		// calculate the redirect URI based on our given base URL
		// the python suite included entity_id in the query string here originally, but this was removed in
		// https://github.com/rohe/oidctest/commit/2c5b8192105d1176eeaf29108fae152b66bcf41c I believe due to
		// https://github.com/openid-certification/oidctest/issues/224
		String initiateLoginUri = baseUrl + "/frontchannel_logout";
		env.putString("frontchannel_logout_uri", initiateLoginUri);

		logSuccess("Created frontchannel_logout_uri URI",
			args("frontchannel_logout_uri", initiateLoginUri));

		return env;
	}

}
