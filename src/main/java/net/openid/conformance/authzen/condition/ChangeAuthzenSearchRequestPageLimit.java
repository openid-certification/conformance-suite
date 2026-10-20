package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Sets page.limit in the search request to a different value, so a request carrying a page
 * token no longer repeats the preceding request's parameters (Section 8.2).
 */
public class ChangeAuthzenSearchRequestPageLimit extends AbstractChangeAuthzenSearchRequest {

	private final int limit;

	public ChangeAuthzenSearchRequestPageLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public String getChangedMember() {
		return "page.limit";
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		JsonObject page = request.getAsJsonObject("page");
		if (page == null) {
			throw error("The search request has no page object to change", args("request", request));
		}
		JsonElement current = page.get("limit");
		if (current != null && current.isJsonPrimitive() && current.getAsJsonPrimitive().isNumber()
			&& OIDFJSON.getNumber(current).intValue() == limit) {
			throw error("The search request already has page.limit " + limit + ", so changing it to that value would leave the request unchanged",
				args("page", page));
		}
		page.addProperty("limit", limit);
		logSuccess("Changed page.limit in the search request", args("page", page));
		return env;
	}
}
