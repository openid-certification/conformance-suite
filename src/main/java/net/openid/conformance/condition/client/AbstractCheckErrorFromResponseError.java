package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public abstract class AbstractCheckErrorFromResponseError extends AbstractCondition {

	protected abstract String getResponseKey();

	protected abstract String[] getExpectedError();

	@Override
	public Environment evaluate(Environment env) {
		if (!env.containsObject(getResponseKey())) {
			throw error("Couldn't find " + getResponseKey());
		}

		String error = getError(env);
		if (Strings.isNullOrEmpty(error)) {
			throw error("Couldn't find error field");
		}

		String[] expected = getExpectedError();
		if (!Arrays.asList(expected).contains(error)) {
			throw error("'error' field has unexpected value", args("expected", expected, "actual", error));
		}

		logSuccess(getResponseKey() + " error returned expected 'error' of '" + error + "'", args("expected", expected));
		return env;
	}

	protected String getError(Environment env) {
		return env.getString(getResponseKey(), "error");
	}
}
