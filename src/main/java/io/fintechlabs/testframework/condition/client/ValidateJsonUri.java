package io.fintechlabs.testframework.condition.client;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateJsonUri extends AbstractCondition {

	private static final String requiredProtocol = "https";
	private static final String errorMessageNotJsonPrimitive = "Specified value is not a Json primative";
	private static final String errorMessageInvalidURL = "Invalid URL. Unable to parse.";

	public ValidateJsonUri(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure,
			String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	public Environment validate(Environment env, String environmentVariable) {

		final String errorMessageWhenNull = "'"+environmentVariable + "' not found (should be a URL)";
		final String errorMessageNotRequiredProtocol = "Expected " + requiredProtocol + " protocol for " + environmentVariable;

		JsonElement serverValue = env.findElement("server", environmentVariable);
		String errorMessage = null;

		if (serverValue == null) {
			errorMessage = errorMessageWhenNull;
		} else {
			if (!serverValue.isJsonPrimitive()) {
				errorMessage = errorMessageNotJsonPrimitive;
			} else {

				try {
					URL theURL = new URL(serverValue.getAsString());
					if (!theURL.getProtocol().equals(requiredProtocol)) {
						errorMessage = errorMessageNotRequiredProtocol;
					}
				} catch (MalformedURLException invalidURL) {
					errorMessage = errorMessageInvalidURL;
				}
			}
		}
		if (errorMessage != null) {
			throw error(errorMessage, args("Protocol Expected:", requiredProtocol, "actual", serverValue));
		}

		logSuccess(environmentVariable + " is valid", args("actual", serverValue));

		return env;
	}

	@Override
	public Environment evaluate(Environment env) {
		// TODO Auto-generated method stub
		return null;
	}

}
