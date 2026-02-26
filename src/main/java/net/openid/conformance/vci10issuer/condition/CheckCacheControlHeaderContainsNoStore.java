package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractValidateResponseCacheHeaders;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckCacheControlHeaderContainsNoStore extends AbstractValidateResponseCacheHeaders {

	@Override
	@PreEnvironment( required = "endpoint_response" )
	public Environment evaluate(Environment env) {

		JsonObject responseHeaders = env.getElementFromObject("endpoint_response", "headers").getAsJsonObject();

		validateCacheHeaders(responseHeaders, "nonce endpoint response");

		return env;
	}
}
