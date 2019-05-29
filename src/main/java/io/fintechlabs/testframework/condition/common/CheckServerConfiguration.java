package io.fintechlabs.testframework.condition.common;

import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckServerConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CheckServerConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment in) {

		// first make sure we've got a "server" object at all
		if (!in.containsObject("server")) {
			throw error("Couldn't find a server configuration at all");
		}

		List<String> lookFor = ImmutableList.of("authorization_endpoint", "token_endpoint", "issuer");

		for (String key : lookFor) {
			ensureString(in, key);
		}

		logSuccess("Found required server configuration keys", args("keys", lookFor));

		return in;
	}

	private void ensureString(Environment in, String path) {
		String string = in.getString("server", path);
		if (Strings.isNullOrEmpty(string)) {
			throw error("Couldn't find required component", args("path", path));
		}
	}

}
