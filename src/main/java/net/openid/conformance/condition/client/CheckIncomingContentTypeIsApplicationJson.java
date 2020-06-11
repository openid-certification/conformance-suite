package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckIncomingContentTypeIsApplicationJson extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_request")
	public Environment evaluate(Environment env) {
		String contentType = env.getString("client_request", "headers.content-type");
		String expected = "application/json";

		if (Strings.isNullOrEmpty(contentType)) {
			throw error("Couldn't find content-type header in incoming request");
		}

		String mimeType = null;
		try {
			mimeType = contentType.split(";")[0].trim();
		} catch (Exception e) {
		}

		if (!expected.equals(mimeType)) {
			throw error("content-type header  does not have the expected value", args("content_type", contentType, "expected", expected));
		}

		logSuccess("Incoming request Content-Type: header has the expected value", args("content_type", contentType, "expected", expected));
		return env;
	}

}
