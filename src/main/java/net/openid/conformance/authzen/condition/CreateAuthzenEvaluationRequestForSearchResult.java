package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Builds the Access Evaluation request that Section 8.1 says a search result should be permitted
 * for: the search request's subject, action, resource and context, with the searched-for entity
 * replaced by the result. Like the other request builders it starts without request headers,
 * so nothing set for the search requests, such as an X-Request-ID, carries over; the caller adds
 * the authentication again.
 */
public class CreateAuthzenEvaluationRequestForSearchResult extends AbstractCondition {

	private final JsonObject searchRequest;

	private final String searchedEntity;

	private final JsonObject result;

	public CreateAuthzenEvaluationRequestForSearchResult(JsonObject searchRequest, String searchedEntity, JsonObject result) {
		this.searchRequest = searchRequest;
		this.searchedEntity = searchedEntity;
		this.result = result;
	}

	@Override
	@PostEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject request = new JsonObject();
		for (String key : new String[] {"subject", "action", "resource", "context"}) {
			if (searchRequest.has(key)) {
				request.add(key, searchRequest.get(key).deepCopy());
			}
		}
		request.add(searchedEntity, result.deepCopy());
		env.putObject("authzen_api_endpoint_request", request);
		env.removeObject("authzen_api_endpoint_request_headers");
		logSuccess("Created an Access Evaluation request for the search result", args("request", request));
		return env;
	}
}
