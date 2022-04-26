package net.openid.conformance.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractValidateResponseCacheHeaders extends AbstractCondition {

	protected void validateCacheHeaders(JsonObject headers, String humanReadableResponseName) {
		String noStore = "no-store";

		if (!headers.has("cache-control")) {
			throw error(humanReadableResponseName + " does not contain 'cache-control' header", args("response_headers", headers));
		}

		JsonElement cacheControl = headers.get("cache-control");

		if (!doesHeaderContainExpectedValue(headers, "cache-control", noStore)) {
			throw error("'cache-control' header in " + humanReadableResponseName + " does not contain expected value.",
				args("expected", noStore, "actual", cacheControl));
		}

		logSuccess("'cache-control' header in " + humanReadableResponseName + " contains expected value.",
					args("cache_control_header", cacheControl));
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
