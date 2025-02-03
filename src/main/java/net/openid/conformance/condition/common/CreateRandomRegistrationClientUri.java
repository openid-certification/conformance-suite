package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateRandomRegistrationClientUri extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_mtls_url")
	@PostEnvironment(required = "registration_client_uri")
	public Environment evaluate(Environment env) {
		String baseMtlsUrl = env.getString("base_mtls_url");

		if (baseMtlsUrl.isEmpty()) {
			throw error("Base MTLS URL is empty");
		}

		// see https://gitlab.com/openid/conformance-suite/wikis/Developers/Build-&-Run#ciba-notification-endpoint
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseMtlsUrl = externalUrlOverride;
		}

		// https://datatracker.ietf.org/doc/html/rfc7592#appendix-B specifies no particular
		// form for this uri - we use a random one (rather than one, say, containing the client_id) to
		// ensure the client does not try to construct the url itself.
		String path = "clienturi/" + RandomStringUtils.secure().nextAlphanumeric(64);
		String queryVar = RandomStringUtils.secure().nextAlphanumeric(16);
		String queryVal = RandomStringUtils.secure().nextAlphanumeric(16);

		JsonObject o = new JsonObject();
		o.addProperty("path", path);
		String fullUrl = baseMtlsUrl + "/" + path + "?" + queryVar + "=" + queryVal;

		o.addProperty("fullUrl", fullUrl);

		env.putObject("registration_client_uri", o);

		log("Created random URL for registration_client_uri", o);

		return env;
	}

}
