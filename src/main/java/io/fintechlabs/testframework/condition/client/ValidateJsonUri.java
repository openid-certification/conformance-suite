package io.fintechlabs.testframework.condition.client;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 *
 * @author ddrysdale
 *
 */

public class ValidateJsonUri extends AbstractCondition {

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
	 *
	 * @param env
	 * @param environmentVariable
	 * @return JsonElement
	 * @throws error
	 *
	 * Get and cache the "server" environment JsonElement
	 *
	 */

	private JsonElement getServerValueOrDie(Environment env, String environmentVariable) {

		if ( ServerValue != null) {
			return ServerValue;
		} else {
			JsonElement serverValue = env.findElement("server", environmentVariable);

			if (serverValue == null) {
				throw error(environmentVariable + ": URL not found");
			} else {
				ServerValue = serverValue;
				return serverValue;
			}
		}
	}

	/***
	 *
	 * @param serverValue
	 * @param environmentVariable
	 * @return URL from the environment
	 *
	 * Get and cache the URL from the Environment variable.
	 */
	private URL extractURLOrDie(JsonElement serverValue, String environmentVariable) {

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
	 *
	 * @param env
	 * @param environmentVariable
	 * @throws error
	 * @return A copy of the Environment
	 *
	 * Validates a specific environment variable URL's protocol
	 */
	public Environment validate(Environment env, String environmentVariable) {

		final String errorMessageNotRequiredProtocol = "Expected " + requiredProtocol + " protocol for " + environmentVariable;

		JsonElement server = getServerValueOrDie(env, environmentVariable);
		URL theURL = extractURLOrDie(server, environmentVariable);

		if (!theURL.getProtocol().equals(requiredProtocol)) {
			throw error(errorMessageNotRequiredProtocol, args("required", requiredProtocol, "actual", server));
		}

		logSuccess(environmentVariable, args("actual", server));

		return env;
	}

	/***
	 *
	 * @param env
	 * @param environmentVariable
	 * @param requiredHostName
	 * @throws error
	 * @return A copy of the Environment
	 *
	 * Validates the host part of the requested URL. Then validates the protocol.
	 *
	 */
	public Environment validateWithHost(Environment env, String environmentVariable, String requiredHostName) {

		JsonElement server = getServerValueOrDie(env, environmentVariable);
		URL theURL = extractURLOrDie(server, environmentVariable);

		if (!theURL.getHost().equals(requiredHostName)) {
			throw error("Invalid Host Name", args("expected", requiredHostName, "actual", theURL.getHost()));
		} else {
			logSuccess("Host Name Passed", args("required", requiredHostName, "actual", theURL.getHost()));
		}

		// Validate the base stuff.
		return validate(env, environmentVariable);

	}

	@Override
	public Environment evaluate(Environment env) {
		// TODO Auto-generated method stub
		return null;
	}

}
