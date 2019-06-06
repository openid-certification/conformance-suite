package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.regex.Pattern;

public class ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint extends AbstractCondition {

	private static final String ERROR_DESCRIPTION_FIELD_PATTERN_VALID = "[\\x20-\\x21\\x23-\\x5B\\x5D-\\x7E]+";

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {

		String errorDescription = env.getString("backchannel_authentication_endpoint_response", "error_description");
		if (!Strings.isNullOrEmpty(errorDescription) && !isValidErrorDescriptionFieldFormat(errorDescription)) {

			throw error("'error_description' field MUST NOT include characters outside the set %x20-21 / %x23-5B / %x5D-7E", args("error_description", errorDescription));
		}

		logSuccess("Backchannel authentication endpoint response error returned valid 'error_description' field", args("error_description", errorDescription));

		return env;
	}

	private boolean isValidErrorDescriptionFieldFormat(String str) {

		Pattern validPattern = Pattern.compile(ERROR_DESCRIPTION_FIELD_PATTERN_VALID);
		if (validPattern.matcher(str).matches()) {
			return true;
		}
		return false;
	}
}
