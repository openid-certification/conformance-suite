package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public abstract class AbstractJsonUriIsValidAndHttps extends AbstractCondition {

	protected static final String requiredProtocol = "https";
	protected static final String errorMessageInvalidURL = "Invalid URL. Unable to parse.";

	/***
	 * Get the named URL value from the "server" environment object. Must not memoize across calls:
	 * callers such as CheckDiscEndpointAllEndpointsAreHttps invoke validate() once per endpoint on
	 * the same instance, so a cached value would make every endpoint validate the first one's URL.
	 */
	protected JsonElement getServerValueOrDie(Environment env, String environmentVariable) {

		JsonElement serverValue = env.getElementFromObject("server", environmentVariable);
		if (serverValue == null) {
			throw error(environmentVariable + ": URL not found");
		}
		return serverValue;
	}

	/***
	 * Parse a URL from {@code serverValue}, which must be a JSON string. {@code fieldName} identifies
	 * the field in the failure messages so they say what was wrong with which value.
	 */
	protected URL extractURLOrDie(JsonElement serverValue, String fieldName) {

		if (!OIDFJSON.isString(serverValue)) {
			throw error(fieldName + " is expected to be a string containing a URL",
				args("actual", serverValue));
		}
		String url = OIDFJSON.getString(serverValue);
		try {
			return URI.create(url).toURL();
		} catch (MalformedURLException | IllegalArgumentException invalidURL) {
			throw error(fieldName + " is not a valid URL",
				args("actual", url, "parse_error", invalidURL.getMessage()));
		}
	}

	/***
	 * Validates a specific environment variable URL's protocol
	 */
	public Environment validate(Environment env, String environmentVariable) {

		JsonElement server = getServerValueOrDie(env, environmentVariable);
		URL theURL = extractURLOrDie(server, environmentVariable);

		if (!theURL.getProtocol().equals(requiredProtocol)) {
			throw error(environmentVariable + " must use the " + requiredProtocol + " scheme",
				args("required", requiredProtocol, "actual_scheme", theURL.getProtocol(), "actual", server));
		}

		logSuccess(environmentVariable, args("actual", server));

		return env;
	}

}
