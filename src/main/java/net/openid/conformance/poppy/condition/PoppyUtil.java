package net.openid.conformance.poppy.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.net.URISyntaxException;

final class PoppyUtil {

	private PoppyUtil() {
	}

	/**
	 * True if the value parses as an absolute URL with the https scheme and a host.
	 */
	static boolean isHttpsUrl(String value) {
		if (value == null) {
			return false;
		}
		try {
			URI uri = new URI(value);
			return "https".equals(uri.getScheme()) && uri.getHost() != null;
		} catch (URISyntaxException e) {
			return false;
		}
	}

	/**
	 * True if the value is exactly {@code https://} plus the host, with the host compared ignoring
	 * case and no port, path, query or trailing slash.
	 */
	static boolean isHttpsOriginOf(String value, String domain) {
		if (!isHttpsUrl(value)) {
			return false;
		}
		URI uri = URI.create(value);
		return uri.getPort() == -1
			&& domain.equalsIgnoreCase(uri.getHost())
			&& (uri.getRawPath() == null || uri.getRawPath().isEmpty())
			&& uri.getRawQuery() == null
			&& uri.getRawFragment() == null
			&& !value.endsWith("/");
	}

	/**
	 * The member as a string, or null if it is absent or not a JSON string.
	 */
	static String stringMember(JsonObject object, String name) {
		JsonElement e = object.get(name);
		return e != null && OIDFJSON.isString(e) ? OIDFJSON.getString(e) : null;
	}

	/**
	 * The member as a long, or null if it is absent or not a JSON number.
	 */
	static Long longMember(JsonObject object, String name) {
		JsonElement e = object.get(name);
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? OIDFJSON.getLong(e) : null;
	}
}
