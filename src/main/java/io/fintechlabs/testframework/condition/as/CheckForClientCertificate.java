package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForClientCertificate extends AbstractCondition {

	public CheckForClientCertificate(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	// note, we don't use the @PreEnvironment check here so we can do a more direct check below
	public Environment evaluate(Environment env) {

		if (env.containsObject("client_certificate")) {
			logSuccess("Found client certificate");
			return env;
		} else {
			throw error("Client certificate not found");
		}

	}

}
