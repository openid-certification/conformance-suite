package net.openid.conformance.authzen.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CreateAuthzenEvaluationsApiRequestSteps extends AbstractConditionSequence {
	private final JsonObject subject;
	private final JsonObject resource;
	private final JsonObject action;
	private final JsonObject context;
	private final JsonArray evaluations;
	private final JsonObject options;

	public CreateAuthzenEvaluationsApiRequestSteps(JsonObject subject, JsonObject resource, JsonObject action, JsonObject context, JsonArray evaluations, JsonObject options) {
		this.subject = subject;
		this.resource = resource;
		this.action = action;
		this.context = context;
		this.evaluations = evaluations;
		this.options = options;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateEmptyAuthzenApiEndpointRequest.class);
		// Per spec 7.1, top-level subject/resource/action are DEFAULTS that each
		// per-evaluation entry merges with its own values, so the top-level
		// objects MAY omit fields that would be required at the evaluation API.
		if(null != subject) {
			callAndStopOnFailure(new CreateAuthzenEvaluationsApiRequestSubjectDefaults(subject), "AUTHZEN-5.1", "AUTHZEN-7.1.1");
		}
		if(null != resource) {
			callAndStopOnFailure(new CreateAuthzenEvaluationsApiRequestResourceDefaults(resource), "AUTHZEN-5.2", "AUTHZEN-7.1.1");
		}
		if(null != action) {
			callAndStopOnFailure(new CreateAuthzenEvaluationsApiRequestActionDefaults(action), "AUTHZEN-5.3", "AUTHZEN-7.1.1");
		}
		if(null != context) {
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestContext(context), "AUTHZEN-5.4", "AUTHZEN-7.1.1");
		}
		if(null != evaluations) {
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestEvaluations(evaluations), "AUTHZEN-7.1");
		}
		if(null != options) {
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestOptions(options), "AUTHZEN-7.1", "AUTHZEN-7.1.2");
		}

	}
}
