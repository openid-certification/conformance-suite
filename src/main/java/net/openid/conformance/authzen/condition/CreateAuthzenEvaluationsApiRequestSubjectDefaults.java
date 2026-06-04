package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;

/**
 * Adds the top-level `subject` defaults object to an Evaluations API request.
 * Per Section 7.1, the top-level subject is a DEFAULT that each
 * evaluation merges with its own subject; the top-level object MAY omit the
 * `type` and `id` fields that {@link CreateAuthzenApiEndpointRequestSubject}
 * normally enforces.
 */
public class CreateAuthzenEvaluationsApiRequestSubjectDefaults extends CreateAuthzenApiEndpointRequestSubject {

	public CreateAuthzenEvaluationsApiRequestSubjectDefaults(JsonObject requestParameter) {
		super(requestParameter);
		this.requiredProperties = new String[]{};
		this.optionalProperties = new String[] {"type", "id", "properties"};
	}
}
