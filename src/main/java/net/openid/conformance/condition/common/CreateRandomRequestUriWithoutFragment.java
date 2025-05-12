package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Creates a URL to retrieve a request object from based on the base_url environment value
 *
 * This version aligns with JAR (RFC9101) and hence doesn't include a fragment.
 */
public class CreateRandomRequestUriWithoutFragment extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "request_uri", strings = "request_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		// see https://gitlab.com/openid/conformance-suite/wikis/Developers/Build-&-Run#ciba-notification-endpoint
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}

		// create a random URL
		//
		// - spec requires full url to be no more than 512 characters
		//
		// - spec does not have any obvious restriction on character set (random alphanumeric used for consistency with
		// python suite)
		//
		// (64 is a relatively arbitrary choice that lies between ~21 characters having a reasonable amount of entropy
		// and the 512 byte upper limit, and appears to match what python does. The spec says clients mustn't use more
		// than 512, but doesn't say servers have to support 512.)
		String path = "requesturi/" + RandomStringUtils.secure().nextAlphanumeric(64);

		JsonObject o = new JsonObject();
		o.addProperty("path", path);
		String fullUrl = baseUrl + "/" + path;
		o.addProperty("fullUrl", fullUrl);

		env.putObject("request_uri", o);
		env.putString("request_uri", fullUrl);

		log("Created random URL for request_uri",
			args("request_uri", o));

		return env;
	}

}
