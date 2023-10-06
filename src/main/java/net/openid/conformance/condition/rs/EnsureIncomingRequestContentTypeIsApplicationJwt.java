package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIncomingRequestContentTypeIsApplicationJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("incoming_request", "headers.content-type");

		if (Strings.isNullOrEmpty(contentType)) {
			throw error("Incoming request does not have a content-type value");
		}
		if (!"application/jwt".equalsIgnoreCase(contentType)) {
			throw error("This endpoint requires content-type application/jwt", args("actual", contentType));
		}
		logSuccess("Client correctly used application/jwt content type");
		return env;
	}

}
