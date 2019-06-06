package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.testmodule.Environment;

public class RemoveMTLSCertificates extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		env.removeObject("mutual_tls_authentication");

		logSuccess("Removed mutual TLS authentication credentials");

		return env;

	}

}
