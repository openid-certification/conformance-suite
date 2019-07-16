package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckTokenEndpointReturnedJsonContentType extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response_headers")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("token_endpoint_response_headers", "content-type");
		if (Strings.isNullOrEmpty(contentType)) {
			throw error("Couldn't find content-type header in token endpoint response");
		}

		String mimeType = null;
		try {
			mimeType = contentType.split(";")[0].trim();
		} catch (Exception e) {
		}

		String expected = "application/json";
		if (expected.equals(mimeType)) {
			logSuccess("Token endpoint Content-Type: header is " + expected);
			return env;
		}

		throw error("Invalid content-type header in token endpoint response", args("expected",	 expected, "actual", contentType));
	}

}
