package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.regex.Pattern;

public class ValidateErrorFromTokenEndpointResponseError extends AbstractCondition {

	private static final String ERROR_FIELD_PATTERN_VALID = "[\\x20-\\x21\\x23-\\x5B\\x5D-\\x7E]+";

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		String error = env.getString("token_endpoint_response", "error");
		if (Strings.isNullOrEmpty(error)) {
			throw error("The authorization server was expected to return an error, but the 'error' field in the response is either null or empty");
		}
		if (!isValidErrorFieldFormat(error)) {
			throw error("'error' field MUST NOT include characters outside the set %x20-21 / %x23-5B / %x5D-7E", args("error", error));
		}
		logSuccess("Token endpoint response error returned valid 'error' field", args("error", error));
		return env;
	}

	private boolean isValidErrorFieldFormat(String str) {
		Pattern validPattern = Pattern.compile(ERROR_FIELD_PATTERN_VALID);
		if (validPattern.matcher(str).matches()) {
			return true;
		}
		return false;
	}
}
