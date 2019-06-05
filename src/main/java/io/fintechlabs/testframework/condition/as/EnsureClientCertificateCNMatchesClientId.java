package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureClientCertificateCNMatchesClientId extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "client_certificate" })
	public Environment evaluate(Environment env) {

		// get the client ID from the configuration
		String expected = env.getString("client", "client_id");
		String actual = env.getString("client_certificate", "subject.dn");

		if (!Strings.isNullOrEmpty(expected) && expected.equals(actual)) {
			logSuccess("Client ID matched", args("client_id", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Mismatch between client ID", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
