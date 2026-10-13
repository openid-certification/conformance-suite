package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Section 8.2: the PEP follows next_token "until the PDP returns a page object in which the value
 * of the next_token field is an empty string, signaling the end of the result set". So the
 * response to a request carrying a page token, which only a paginating PDP receives, must carry a
 * page object, including on the last page (certification scenario 7.5.2: "page is present and is
 * an object"). The page object's own structure is checked by {@link EnsureValidSearchResponsePage}.
 */
public class EnsureSearchResponseHasPageAfterPageToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_request", "authzen_search_endpoint_response"})
	public Environment evaluate(Environment env) {
		JsonElement token = env.getElementFromObject("authzen_api_endpoint_request", "page.token");
		if (token == null || !token.isJsonPrimitive() || !token.getAsJsonPrimitive().isString()
			|| OIDFJSON.getString(token).isEmpty()) {
			log("The search request carried no page token");
			return env;
		}
		JsonElement page = env.getElementFromObject("authzen_search_endpoint_response", "page");
		if (page == null || !page.isJsonObject()) {
			throw error("The search request carried the page token from the PDP's previous response, but the response has no page object; "
					+ "a paginated search ends with a page object whose next_token is an empty string",
				args("page_token", token, "response", env.getObject("authzen_search_endpoint_response")));
		}
		logSuccess("The response to a request carrying a page token contains a page object", args("page", page));
		return env;
	}
}
