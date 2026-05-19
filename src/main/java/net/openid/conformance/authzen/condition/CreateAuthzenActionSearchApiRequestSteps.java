package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CreateAuthzenActionSearchApiRequestSteps extends AbstractConditionSequence {
	private final JsonObject subject;
	private final JsonObject resource;
	private final JsonObject context;
	private final JsonObject page;

	public CreateAuthzenActionSearchApiRequestSteps(JsonObject subject, JsonObject resource, JsonObject context, JsonObject page) {
		this.subject = subject;
		this.resource = resource;
		this.context = context;
		this.page = page;
	}

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateEmptyAuthzenApiEndpointRequest.class);
		callAndStopOnFailure(new CreateAuthzenApiEndpointRequestSubject(subject), "AUTHZEN-5.1", "AUTHZEN-8.6.1");
		callAndStopOnFailure(new CreateAuthzenApiEndpointRequestResource(resource), "AUTHZEN-5.2", "AUTHZEN-8.6.1");
		if(null != context) {
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestContext(context), "AUTHZEN-5.4", "AUTHZEN-8.6.1");
		}
		if(null != page) {
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestSearchPage(page), "AUTHZEN-8.2.1", "AUTHZEN-8.6.1");
		}
	}
}
