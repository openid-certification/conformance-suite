package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class CheckForClientCertificate extends AbstractCondition {

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
