package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;

/**
 * Adds the top-level `resource` defaults object to an Evaluations API request.
 * Per spec section 7.1, the top-level resource is a DEFAULT that each
 * evaluation merges with its own resource; the top-level object MAY omit the
 * `type` and `id` fields that {@link CreateAuthzenApiEndpointRequestResource}
 * normally enforces.
 */
public class CreateAuthzenEvaluationsApiRequestResourceDefaults extends CreateAuthzenApiEndpointRequestResource {

	public CreateAuthzenEvaluationsApiRequestResourceDefaults(JsonObject requestParameter) {
		super(requestParameter);
		this.requiredProperties = new String[]{};
		this.optionalProperties = new String[] {"type", "id", "properties"};
	}
}
