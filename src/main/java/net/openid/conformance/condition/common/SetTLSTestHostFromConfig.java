package net.openid.conformance.condition.common;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractSetTLSTestHost;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
