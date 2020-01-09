package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.net.URL;
import java.util.regex.Pattern;

public abstract class AbstractValidateErrorUriFromResponseError extends AbstractCondition {
	private static final String ERROR_URI_FIELD_PATTERN_VALID = "[\\x21\\x23-\\x5B\\x5D-\\x7E]+";

	protected Environment checkErrorUri(Environment env, String where) {
		String errorUri = env.getString(where, "error_uri");

		if (Strings.isNullOrEmpty(errorUri)) {
			logSuccess(where + " did not include optional 'error_uri' field");
			return env;
		}

		if (!isValidUriSyntax(errorUri)) {
			throw error("'error_uri' field MUST conform to the URI-reference syntax", args("error_uri", errorUri));
		}
		if (!isValidErrorUriFieldFormat(errorUri)) {
			throw error("'error_uri' field MUST NOT include characters outside the set %x21 / %x23-5B / %x5D-7E", args("error_uri", errorUri));
		}

		logSuccess(where + " returned valid 'error_uri' field", args("error_uri", errorUri));
		return env;
	}

	private boolean isValidErrorUriFieldFormat(String str) {
		Pattern validPattern = Pattern.compile(ERROR_URI_FIELD_PATTERN_VALID);
		if (validPattern.matcher(str).matches()) {
			return true;
		}
		return false;
	}

	private boolean isValidUriSyntax(String errorUri) {
		try {
			new URL(errorUri).toURI();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
