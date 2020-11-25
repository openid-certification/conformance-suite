package net.openid.conformance.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
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

		JsonElement cacheControl = headers.get("cache-control");

		if (!doesHeaderContainExpectedValue(headers, "cache-control", noStore)) {
			throw error("'cache-control' header in " + humanReadableResponseName + " does not contain expected value.",
				args("expected", noStore, "actual", cacheControl));
		}
		if (cacheControlMustHaveNoCache) {
			// the backchannel logout specs require this, but RFC6749 does not
			if (!doesHeaderContainExpectedValue(headers, "cache-control", noCache)) {
				throw error("'cache-control' header in " + humanReadableResponseName + " does not contain expected value.",
					args("expected", noCache, "actual", cacheControl));
			}
		}

		JsonElement pragma = headers.get("pragma");
		if (!doesHeaderContainExpectedValue(headers, "pragma", noCache)) {
			throw error("'pragma' header in "+humanReadableResponseName+" does not contain expected value.",
						args("expected", noCache, "actual", pragma));
		}

		logSuccess("'pragma' and 'cache-control' headers in " + humanReadableResponseName + " contain expected values.",
					args("cache_control_header", cacheControl, "pragma_header", pragma));
	}

	private boolean doesHeaderContainExpectedValue(String header, String expected) {
		if (Strings.isNullOrEmpty(header)) {
			return false;
		}

		for (String piece : header.split(",")) {
			if (piece.trim().equals(expected)) {
				return true;
			}
		}

		return false;
	}

	private boolean doesHeaderContainExpectedValue(JsonObject headers, String headerName, String expected) {
		JsonElement headerJson = headers.get(headerName);
		if (headerJson.isJsonArray()) {
			for (JsonElement el : headerJson.getAsJsonArray()) {
				String header = OIDFJSON.getString(el);
				if (doesHeaderContainExpectedValue(header, expected)) {
					return true;
				}
			}
			return false;
		}
		String header = OIDFJSON.getString(headerJson);
		return doesHeaderContainExpectedValue(header, expected);
	}
}
