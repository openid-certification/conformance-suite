package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Adds the next_token of the previous search response to the search request as page.token
 * (Section 8.2), keeping the request's other page members. The request builders add it
 * themselves; this is for a request sent as the module's payload was written, which would
 * otherwise ask for the first page again.
 */
public class AddAuthzenSearchPageTokenToRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		String token = env.getString("authzen_search_endpoint_request_page_token");
		if (Strings.isNullOrEmpty(token)) {
			log("No page token from the previous response to add to the search request");
			return env;
		}
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		JsonElement pageElement = request.get("page");
		JsonObject page;
		if (pageElement != null && pageElement.isJsonObject()) {
			page = pageElement.getAsJsonObject();
		} else {
			page = new JsonObject();
			request.add("page", page);
		}
		page.addProperty("token", token);
		logSuccess("Added the page token from the previous response to the search request", args("page", page));
		return env;
	}
}
