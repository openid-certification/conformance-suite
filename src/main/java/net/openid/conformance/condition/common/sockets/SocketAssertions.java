package net.openid.conformance.condition.common.sockets;

import net.openid.conformance.condition.FunctionalCondition;

import java.util.Map;
import java.util.Objects;

public class SocketAssertions {

	public static FunctionalCondition.Body assertHttpStatusCode(Integer expectedStatus) {

		return (env, c) -> {
			Integer statusCode = env.getInteger("last_http_call", "status");
			if (!Objects.equals(statusCode, expectedStatus)) {
				throw c.error("Invalid http status", Map.of("actual", statusCode, "expected", expectedStatus));
			}
			c.logSuccess(env.getString("last_http_call_description") + " http status valid", Map.of("status", statusCode));
		};
	}

}
