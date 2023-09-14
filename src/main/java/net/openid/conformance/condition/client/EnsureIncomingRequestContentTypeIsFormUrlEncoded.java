package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIncomingRequestContentTypeIsFormUrlEncoded extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		String contentType = env.getString("incoming_request", "headers.content-type");
		String expected = "application/x-www-form-urlencoded";

		if (Strings.isNullOrEmpty(contentType)) {
			throw error("Incoming request does not have a content-type value");
		}
		if (!contentType.equals(expected)) {
			throw error("Incoming content-type header does not have the expected value", args("content_type", contentType, "expected", expected));
		}

		logSuccess("Incoming content-type header has the expected value", args("content_type", contentType, "expected", expected));
		return env;
	}

}
