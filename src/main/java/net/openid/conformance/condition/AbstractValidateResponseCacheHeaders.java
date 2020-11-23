package net.openid.conformance.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractValidateResponseCacheHeaders extends AbstractCondition {

	protected void validateCacheHeaders(JsonObject headers, String humanReadableResponseName, boolean cacheControlMustHaveNoCache) {
		String noStore = "no-store";
		String noCache = "no-cache";

		if (!headers.has("cache-control")) {
			throw error(humanReadableResponseName + " does not contain 'cache-control' header", args("response_headers", headers));
		}

		if (!headers.has("pragma")) {
			throw error(humanReadableResponseName + " does not contain 'pragma' header", args("response_headers", headers));
		}

		String cacheControl = OIDFJSON.getString(headers.get("cache-control"));


		if (Strings.isNullOrEmpty(cacheControl) || !doesHeaderContainExpectedValue(cacheControl, noStore)) {
			throw error("'cache-control' header in " + humanReadableResponseName + " does not contain expected value.",
				args("expected", noStore, "actual", cacheControl));
		}
		if (cacheControlMustHaveNoCache) {
			// the backchannel logout specs require this, but RFC6749 does not
			if (Strings.isNullOrEmpty(cacheControl) || !doesHeaderContainExpectedValue(cacheControl, noCache)) {
				throw error("'cache-control' header in " + humanReadableResponseName + " does not contain expected value.",
					args("expected", noCache, "actual", cacheControl));
			}
		}

		String pragma = OIDFJSON.getString(headers.get("pragma"));
		if (Strings.isNullOrEmpty(pragma) || !doesHeaderContainExpectedValue(pragma, noCache)) {
			throw error("'pragma' header in "+humanReadableResponseName+" does not contain expected value.",
						args("expected", noCache, "actual", pragma));
		}

		logSuccess("'pragma' and 'cache-control' headers in " + humanReadableResponseName + " contain expected values.",
					args("cache_control_header", cacheControl, "pragma_header", pragma));
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
