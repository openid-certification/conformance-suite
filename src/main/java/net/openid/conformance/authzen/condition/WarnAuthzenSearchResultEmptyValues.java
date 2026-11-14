package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags search results whose `type`, `id` or, for an action, `name` is an empty string. Section 5
 * only requires these to be strings; the working group answered openid/authzen#781 that they MUST
 * NOT be empty, which openid/authzen PR 792 adds to Sections 5.1, 5.2 and 5.3. Until that text is
 * published an empty value is not a failure, so this is intended to be called at WARNING. Absent
 * and non-string values are left to the search response validators.
 */
public class WarnAuthzenSearchResultEmptyValues extends AbstractCondition {

	private static final String[] MEMBERS = {"type", "id", "name"};

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement results = env.getElementFromObject("authzen_search_endpoint_response", "results");
		List<JsonObject> withEmptyValues = new ArrayList<>();
		if (results != null && results.isJsonArray()) {
			for (JsonElement result : results.getAsJsonArray()) {
				if (result.isJsonObject() && hasEmptyValue(result.getAsJsonObject())) {
					withEmptyValues.add(result.getAsJsonObject());
				}
			}
		}
		if (!withEmptyValues.isEmpty()) {
			throw error("The search results include an entity whose type, id or name is an empty string. The working group "
				+ "answered that these values must not be empty; until the errata saying so are published, this is reported rather "
				+ "than failed", args("results_with_empty_values", withEmptyValues));
		}
		logSuccess("No search result has an empty type, id or name");
		return env;
	}

	private static boolean hasEmptyValue(JsonObject result) {
		for (String member : MEMBERS) {
			JsonElement value = result.get(member);
			if (OIDFJSON.isString(value) && OIDFJSON.getString(value).isEmpty()) {
				return true;
			}
		}
		return false;
	}
}
