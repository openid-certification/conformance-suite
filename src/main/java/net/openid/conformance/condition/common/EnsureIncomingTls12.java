package net.openid.conformance.condition.common;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIncomingTls12 extends AbstractCondition {

	private static final String TLS_12 = "TLSv1.2";

	@Override
	@PreEnvironment(required = "client_request")
	public Environment evaluate(Environment env) {

		String protocol = env.getString("client_request", "headers.x-ssl-protocol");

		if (Strings.isNullOrEmpty(protocol)) {
			throw error("TLS Protocol not found");
		}

		if (protocol.equals(TLS_12)) {
			logSuccess("Found TLS 1.2 connection");
			return env;
		} else {
			throw error("Found disallowed TLS connection", args("expected", TLS_12, "actual", protocol));
		}

	}

}
