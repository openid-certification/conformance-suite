package io.fintechlabs.testframework.condition.common;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractSetTLSTestHost;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class SetTLSTestHostFromConfig extends AbstractSetTLSTestHost {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "tls")
	public Environment evaluate(Environment env) {

		String tlsTestHost = env.getString("config", "tls.testHost");
		Integer tlsTestPort = env.getInteger("config", "tls.testPort");

		if (Strings.isNullOrEmpty(tlsTestHost)) {
			throw error("Couldn't find host to connect for TLS in config");
		}

		if (tlsTestPort == null) {
			throw error("Couldn't find port to connect for TLS in config");
		}

		return setTLSTestHost(env, tlsTestHost, (int) tlsTestPort);
	}

}
