package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckBackchannelAuthenticationEndpointContentType extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("backchannel_authentication_endpoint_response_headers", "content-type");
		if (Strings.isNullOrEmpty(contentType)) {
			throw error("Couldn't find content-type header in backchannel authentication endpoint response");
		}

		String mimeType = null;
		try {
			mimeType = contentType.split(";")[0].trim();
		} catch (Exception e) {
		}

		String expected = "application/json";
		if (!expected.equals(mimeType)) {
			throw error("Invalid content-type header in backchannel authentication endpoint response", args("expected",	 expected, "actual", contentType));

		}

		logSuccess("Backchannel authentication endpoint Content-Type: header is " + expected);
		return env;
	}
}
