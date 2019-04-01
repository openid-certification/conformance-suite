package io.fintechlabs.testframework.condition.client;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public abstract class ValidateJsonUri extends AbstractCondition {

	private static final String requiredProtocol = "https";
	private static final String errorMessageNotJsonPrimitive = "Specified value is not a Json primative";
	private static final String errorMessageInvalidURL = "Invalid URL. Unable to parse.";

	private JsonElement ServerValue = null;
	private URL extractedUrl = null;

	public ValidateJsonUri(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure,
			String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

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
					extractedUrl = new URL(serverValue.getAsString());
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
