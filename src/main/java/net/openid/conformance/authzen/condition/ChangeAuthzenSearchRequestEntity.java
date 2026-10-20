package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Replaces one entity of the search request (subject, resource, action or context) with another,
 * so a request carrying a page token no longer repeats the preceding request's entities
 * (Section 8.2).
 */
public class ChangeAuthzenSearchRequestEntity extends AbstractChangeAuthzenSearchRequest {

	private final String entity;

	private final JsonObject replacement;

	public ChangeAuthzenSearchRequestEntity(String entity, JsonObject replacement) {
		this.entity = entity;
		this.replacement = replacement;
	}

	@Override
	public String getChangedMember() {
		return entity;
	}

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		if (!request.has(entity)) {
			throw error("The search request has no " + entity + " to change", args("request", request));
		}
		if (request.get(entity).equals(replacement)) {
			throw error("The replacement " + entity + " is the same as the one in the search request", args(entity, replacement));
		}
		request.add(entity, replacement.deepCopy());
		logSuccess("Changed " + entity + " in the search request", args(entity, replacement));
		return env;
	}
}
