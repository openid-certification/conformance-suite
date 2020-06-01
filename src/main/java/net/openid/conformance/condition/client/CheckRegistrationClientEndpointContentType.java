package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckRegistrationClientEndpointContentType extends AbstractCondition {

	@Override
	@PreEnvironment(required = "registration_client_endpoint_response")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("registration_client_endpoint_response", "headers.content-type");
		if (Strings.isNullOrEmpty(contentType)) {
			throw error("Couldn't find content-type header in registration_client_endpoint_response");
		}

		String mimeType = null;
		try {
			mimeType = contentType.split(";")[0].trim();
		} catch (Exception e) {
		}

		String expected = "application/json";
		if (!expected.equals(mimeType)) {
			throw error("Invalid content-type header in registration_client_endpoint_response", args("expected",	 expected, "actual", contentType));

		}

		logSuccess("registration_client_endpoint_response Content-Type: header is " + expected);
		return env;
	}
}
