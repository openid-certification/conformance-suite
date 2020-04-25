package net.openid.conformance.condition.as.logout;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureBackChannelLogoutEndpointResponseContainsCacheHeaders extends AbstractCondition {

	@Override
	@PreEnvironment( required = "backchannel_logout_endpoint_response" )
	public Environment evaluate(Environment env) {
		JsonObject headers = env.getElementFromObject("backchannel_logout_endpoint_response", "headers").getAsJsonObject();

		String noStore = "no-store";
		String noCache = "no-cache";

		if (!headers.has("cache-control")) {
			throw error("Response does not contain 'cache-control' header", args("response_headers", headers));
		}

		if (!headers.has("pragma")) {
			throw error("Response does not contain 'pragma' header", args("response_headers", headers));
		}

		String cacheControl = OIDFJSON.getString(headers.get("cache-control"));


		if (Strings.isNullOrEmpty(cacheControl) || !doesHeaderContainExpectedValue(cacheControl, noStore)) {
			throw error("'cache-control' header does not contain expected value.",
				args("expected", noStore, "actual", cacheControl));
		}
		if (Strings.isNullOrEmpty(cacheControl) || !doesHeaderContainExpectedValue(cacheControl, noCache)) {
			throw error("'cache-control' header does not contain expected value.",
				args("expected", noCache, "actual", cacheControl));
		}

		String pragma = OIDFJSON.getString(headers.get("pragma"));
		if (Strings.isNullOrEmpty(pragma) || !doesHeaderContainExpectedValue(pragma, noCache)) {
			throw error("'pragma' header does not contain expected value.",
						args("expected", noCache, "actual", pragma));
		}

		logSuccess("'pragma' and 'cache-control' headers in RP backchannel_logout_uri response contain expected values.",
					args("cache_control_header", cacheControl, "pragma_header", pragma));

		return env;
	}

	private boolean doesHeaderContainExpectedValue(String header, String expected) {
		if (header == null || header.isEmpty()) {
			return false;
		}

		for (String piece : header.split(",")) {
			if (piece.trim().equals(expected)) {
				return true;
			}
		}

		return false;
	}
}
