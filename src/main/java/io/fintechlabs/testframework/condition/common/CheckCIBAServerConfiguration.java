package io.fintechlabs.testframework.condition.common;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.List;

public class CheckCIBAServerConfiguration extends AbstractCondition {

	public CheckCIBAServerConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment in) {

		List<String> lookFor = ImmutableList.of(
			"backchannel_authentication_endpoint",
			"token_endpoint",
			"issuer");

		for (String key : lookFor) {
			ensureString(in, key);
		}

		logSuccess("Found required server configuration keys", args("keys", lookFor));

		return in;
	}

	private void ensureString(Environment in, String path) {
		String string = in.getString("server", path);
		if (Strings.isNullOrEmpty(string)) {
			throw error("Couldn't find required parameter", args("path", path));
		}
	}

}
