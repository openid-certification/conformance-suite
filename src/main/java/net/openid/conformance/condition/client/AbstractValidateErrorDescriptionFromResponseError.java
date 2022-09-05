package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.regex.Pattern;

public abstract class AbstractValidateErrorDescriptionFromResponseError extends AbstractCondition {

	private static final String ERROR_DESCRIPTION_FIELD_PATTERN_VALID = "[\\x09\\x0A\\x0D\\x20-\\x21\\x23-\\x5B\\x5D-\\x7E]+";

	protected Environment validateErrorDescription(Environment env, String endpointResponseKey) {
		String errorDescription = env.getString(endpointResponseKey, "error_description");

		if (Strings.isNullOrEmpty(errorDescription)) {
			logSuccess(endpointResponseKey + " did not include optional 'error_description' field");
			return env;
		}
		if (!isValidErrorDescriptionFieldFormat(errorDescription)) {
			throw error("'error_description' field MUST NOT include characters outside the set %09-0A (Tab and LF) / %x0D (CR) / %x20-21 / %x23-5B / %x5D-7E", args("error_description", errorDescription));
		}
		logSuccess(endpointResponseKey + " error returned valid 'error_description' field", args("error_description", errorDescription));
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
