package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CreateAuthzenSubjectSearchApiRequestSteps extends AbstractConditionSequence {
	private final JsonObject subject;
	private final JsonObject resource;
	private final JsonObject action;
	private final JsonObject context;

	private final JsonObject page;

	public CreateAuthzenSubjectSearchApiRequestSteps(JsonObject subject, JsonObject resource, JsonObject action, JsonObject context, JsonObject page) {
		this.subject = subject;
		this.resource = resource;
		this.action = action;
		this.context = context;
		this.page = page;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateEmptyAuthzenApiEndpointRequest.class);
		callAndStopOnFailure(new CreateAuthzenApiEndpointRequestSearchSubject(subject), "AUTHZEN-8.4.1");
		callAndStopOnFailure(new CreateAuthzenApiEndpointRequestResource(resource), "AUTHZEN-5.2", "AUTHZEN-8.4.1");
		callAndStopOnFailure(new CreateAuthzenApiEndpointRequestAction(action), "AUTHZEN-5.3", "AUTHZEN-8.4.1");
		if(null != context) {
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestContext(context), "AUTHZEN-5.4", "AUTHZEN-8.4.1");
		}
		if(null != page) {
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestSearchPage(context), "AUTHZEN-8.2.1", "AUTHZEN-8.4.1");
		}
	}
}
