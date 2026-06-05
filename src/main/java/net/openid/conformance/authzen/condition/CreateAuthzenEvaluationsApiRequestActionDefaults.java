package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;

/**
 * Adds the top-level `action` defaults object to an Evaluations API request.
 * Per Section 7.1, the top-level action is a DEFAULT that each
 * evaluation merges with its own action; the top-level object MAY omit the
 * `name` field that {@link CreateAuthzenApiEndpointRequestAction} normally
 * enforces.
 */
public class CreateAuthzenEvaluationsApiRequestActionDefaults extends CreateAuthzenApiEndpointRequestAction {

	public CreateAuthzenEvaluationsApiRequestActionDefaults(JsonObject requestParameter) {
		super(requestParameter);
		this.requiredProperties = new String[]{};
		this.optionalProperties = new String[] {"name", "properties"};
	}
}
