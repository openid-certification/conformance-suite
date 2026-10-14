package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the status and headers of the AuthZEN API response in {@code authzen_api_endpoint_response}.
 */
public abstract class AbstractAuthzenResponseHeaderCondition extends AbstractCondition {

	protected Integer responseStatus(Environment env) {
		return env.getInteger("authzen_api_endpoint_response", "status");
	}

	/**
	 * Returns every value of the named response header, matching the name case-insensitively.
	 */
	protected List<String> responseHeaderValues(Environment env, String name) {
		List<String> values = new ArrayList<>();
		JsonElement headersElement = env.getElementFromObject("authzen_api_endpoint_response", "headers");
		if (headersElement == null || !headersElement.isJsonObject()) {
			return values;
		}
		JsonObject headers = headersElement.getAsJsonObject();
		String lower = name.toLowerCase(Locale.ROOT);
		for (String key : headers.keySet()) {
			if (!key.toLowerCase(Locale.ROOT).equals(lower)) {
				continue;
			}
			JsonElement value = headers.get(key);
			if (value.isJsonArray()) {
				JsonArray array = value.getAsJsonArray();
				for (JsonElement item : array) {
					values.add(OIDFJSON.getString(item));
				}
			} else if (value.isJsonPrimitive()) {
				values.add(OIDFJSON.getString(value));
			}
		}
		return values;
	}
}
