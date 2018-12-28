package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class RemoveMTLSCertificates extends AbstractCondition {

	public RemoveMTLSCertificates(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {

		env.removeObject("mutual_tls_authentication");

		logSuccess("Removed mutual TLS authentication credentials");

		return env;

	}

}
