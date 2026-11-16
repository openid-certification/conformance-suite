package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

/**
 * Section 8.3: a Search API response has page, context and results. Section 8.2.2: page has
 * next_token, count, total and properties. Each result is an entity of the searched type
 * (Section 5), whose members subclasses define. Reads {@code authzen_search_endpoint_response}.
 */
public abstract class AbstractWarnAuthzenSearchResponseUnknownMembers extends AbstractWarnAuthzenUnknownMembers {

	private static final Set<String> RESPONSE_MEMBERS = Set.of("page", "context", "results");

	private static final Set<String> PAGE_MEMBERS = Set.of("next_token", "count", "total", "properties");

	protected abstract Set<String> getEntityMembers();

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("authzen_search_endpoint_response");
		Map<String, Set<String>> unknown = newUnknownMembers();
		collectUnknownMembers(response, "$", RESPONSE_MEMBERS, unknown);
		collectUnknownMembers(response.get("page"), "$.page", PAGE_MEMBERS, unknown);
		collectUnknownMembersOfEach(response.get("results"), "$.results", getEntityMembers(), unknown);
		return reportUnknownMembers(env, unknown, "Search API response");
	}
}
