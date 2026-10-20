package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckCallbackContentTypeIsFormUrlEncoded extends AbstractCondition {

	@Override
	@PreEnvironment(required = "callback_headers")
	public Environment evaluate(Environment env) {
		String contentType = env.getString("callback_headers", "content-type");
		String expected = "application/x-www-form-urlencoded";

		if (contentType == null) {
			throw error("content-type header to redirect_uri is missing", args("expected", expected));
		}

		// parameters such as charset are not part of the media type, whose name is case-insensitive (RFC 9110 Section 8.3.1)
		String mimeType = AbstractCheckEndpointContentTypeReturned.getMimeTypeFromContentType(contentType);
		if (!expected.equalsIgnoreCase(mimeType)) {
			throw error("content-type header to redirect_uri does not have the expected value", args("content_type", contentType, "expected", expected));
		}

		logSuccess("content-type header to redirect_uri has the expected value", args("content_type", contentType, "expected", expected));
		return env;
	}

}
