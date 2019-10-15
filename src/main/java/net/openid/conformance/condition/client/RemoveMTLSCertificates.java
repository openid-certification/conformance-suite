package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RemoveMTLSCertificates extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		env.removeObject("mutual_tls_authentication");

		logSuccess("Removed mutual TLS authentication credentials");

		return env;

	}

}
