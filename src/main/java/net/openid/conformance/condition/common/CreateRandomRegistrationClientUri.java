package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.runner.TestDispatcher;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateRandomRegistrationClientUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "registration_client_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}
		baseUrl = baseUrl.replaceFirst(TestDispatcher.TEST_PATH, TestDispatcher.TEST_MTLS_PATH);

		// see https://gitlab.com/openid/conformance-suite/wikis/Developers/Build-&-Run#ciba-notification-endpoint
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}

		// https://datatracker.ietf.org/doc/html/rfc7592#appendix-B specifies no particular
		// form for this uri - we use a random one (rather than one, say, containing the client_id) to
		// ensure the client does not try to construct the url itself.
		String path = "clienturi/" + RandomStringUtils.randomAlphanumeric(64);
		String queryVar = RandomStringUtils.randomAlphanumeric(16);
		String queryVal = RandomStringUtils.randomAlphanumeric(16);

		JsonObject o = new JsonObject();
		o.addProperty("path", path);
		String fullUrl = baseUrl + "/" + path + "?" + queryVar + "=" + queryVal;
		o.addProperty("fullUrl", fullUrl);

		env.putObject("registration_client_uri", o);

		log("Created random URL for registration_client_uri", o);

		return env;
	}

}
