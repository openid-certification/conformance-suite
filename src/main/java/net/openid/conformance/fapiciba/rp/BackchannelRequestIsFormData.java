package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelRequestIsFormData extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("backchannel_endpoint_http_request", "headers.content-type");
		String expected = "application/x-www-form-urlencoded";

		String mimeType = null;
		try {
			mimeType = contentType.split(";")[0].trim();
		} catch (Exception ignored) { }

		if (!expected.equals(mimeType)) {
			throw error("content-type header  does not have the expected value", args("content_type", contentType, "expected", expected));
		}

		logSuccess("Backchannel authentication request has Content-Type 'application/x-www-form-urlencoded'");

		return env;
	}


}
