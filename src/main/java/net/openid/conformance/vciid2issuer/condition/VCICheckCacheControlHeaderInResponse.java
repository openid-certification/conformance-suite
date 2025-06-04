package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpHeaders;

public class VCICheckCacheControlHeaderInResponse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject responseHeaders = env.getElementFromObject("endpoint_response", "headers").getAsJsonObject();
		if (!responseHeaders.has(HttpHeaders.CACHE_CONTROL.toLowerCase())) {
			throw error("Could not find Cache-Control in response headers", args("response_headers", responseHeaders));
		}

		String cacheControlHeader = OIDFJSON.getString(responseHeaders.get(HttpHeaders.CACHE_CONTROL.toLowerCase()));
		if (!"no-store".equals(cacheControlHeader)) {
			throw error("Cache-Control header in response headers must be 'no-store'.", args("response_headers", responseHeaders));
		}

		logSuccess("Found expected cache-control header in endpoint response", args("response_headers", responseHeaders, "cache-control", cacheControlHeader));

		return env;
	}
}
