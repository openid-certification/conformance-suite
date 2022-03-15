package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class AbstractJsonUriIsValidAndHttps extends AbstractCondition {

	protected static final String requiredProtocol = "https";
	private static final String errorMessageNotJsonPrimitive = "Specified value is not a Json primitive";
	protected static final String errorMessageInvalidURL = "Invalid URL. Unable to parse.";

	private JsonElement ServerValue = null;
	private URL extractedUrl = null;

	/***
	 * Get and cache the "server" environment JsonElement
	 */

	protected JsonElement getServerValueOrDie(Environment env, String environmentVariable) {

		if ( ServerValue != null) {
			return ServerValue;
		} else {
			JsonElement serverValue = env.getElementFromObject("server", environmentVariable);

			if (serverValue == null) {
				throw error(environmentVariable + ": URL not found");
			} else {
				ServerValue = serverValue;
				return serverValue;
			}
		}
	}

	/***
	 * Get and cache the URL from the Environment variable.
	 */
	protected URL extractURLOrDie(JsonElement serverValue) {

		if (extractedUrl != null) {
			return extractedUrl;
		} else {
			if (!serverValue.isJsonPrimitive()) {
				throw error(errorMessageNotJsonPrimitive);
			} else {
				try {
					extractedUrl = new URL(OIDFJSON.getString(serverValue));
					return extractedUrl;
				} catch (MalformedURLException invalidURL) {
					throw error(errorMessageInvalidURL);
				}
			}
		}
	}

	/***
	 * Validates a specific environment variable URL's protocol
	 */
	public Environment validate(Environment env, String environmentVariable) {

		final String errorMessageNotRequiredProtocol = "Expected " + requiredProtocol + " protocol for " + environmentVariable;

		JsonElement server = getServerValueOrDie(env, environmentVariable);
		URL theURL = extractURLOrDie(server);

		if (!theURL.getProtocol().equals(requiredProtocol)) {
			throw error(errorMessageNotRequiredProtocol, args("required", requiredProtocol, "actual", server));
		}

		logSuccess(environmentVariable, args("actual", server));

		return env;
	}

}
