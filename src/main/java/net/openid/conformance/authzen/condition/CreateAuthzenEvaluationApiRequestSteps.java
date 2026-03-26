package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CreateAuthzenEvaluationApiRequestSteps extends AbstractConditionSequence {
	private final JsonObject subject;
	private final JsonObject resource;
	private final JsonObject action;
	private final JsonObject context;

	public CreateAuthzenEvaluationApiRequestSteps(JsonObject subject, JsonObject resource, JsonObject action, JsonObject context) {
		this.subject = subject;
		this.resource = resource;
		this.action = action;
		this.context = context;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateEmptyAuthzenApiEndpointRequest.class);
		callAndStopOnFailure(new CreateAuthzenApiEndpointRequestSubject(subject), "AUTHZEN-5.1", "AUTHZEN-6.1");
		callAndStopOnFailure(new CreateAuthzenApiEndpointRequestResource(resource), "AUTHZEN-5.2", "AUTHZEN-6.1");
		callAndStopOnFailure(new CreateAuthzenApiEndpointRequestAction(action), "AUTHZEN-5.3", "AUTHZEN-6.1");
		callAndStopOnFailure(new CreateAuthzenApiEndpointRequestContext(context), "AUTHZEN-5.4", "AUTHZEN-6.1");
	}
}
