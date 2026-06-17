package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIncomingTls13 extends AbstractCondition {

	private static final String TLS_13 = "TLSv1.3";

	@Override
	@PreEnvironment(required = "client_request")
	public Environment evaluate(Environment env) {

		String protocol = env.getString("client_request", "headers.x-ssl-protocol");

		if (Strings.isNullOrEmpty(protocol)) {
			throw error("TLS protocol not found; this header should have been set by the nginx proxy");
		}

		if (protocol.equals(TLS_13)) {
			logSuccess("TLS 1.3 in use");
			return env;
		}

		throw error("Client doesn't support TLS 1.3", args("actual", protocol));

	}

}
