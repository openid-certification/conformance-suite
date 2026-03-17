package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.AddActionToAuthzenApiEndpointRequest;
import net.openid.conformance.authzen.condition.AddContextToAuthzenApiEndpointRequest;
import net.openid.conformance.authzen.condition.AddResourceToAuthzenApiEndpointRequest;
import net.openid.conformance.authzen.condition.AddSubjectToAuthzenApiEndpointRequest;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestAction;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestContext;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestResource;
import net.openid.conformance.authzen.condition.CreateAuthzenApiEndpointRequestSubject;
import net.openid.conformance.authzen.condition.CreateEmptyAuthzenApiEndpointRequest;
import net.openid.conformance.authzen.condition.EnsureDecisionResponseTrue;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

public abstract class AbstractAuthzenPDPEvaluationTest extends AbstractAuthzenPDPTest {

	public static class CreateAuthzenApiRequestSteps extends AbstractConditionSequence {
		private JsonObject subject;
		private JsonObject resource;
		private JsonObject action;
		private JsonObject context;
		CreateAuthzenApiRequestSteps(JsonObject subject, JsonObject resource, JsonObject action, JsonObject context) {
			this.subject = subject;
			this.resource = resource;
			this.action = action;
			this.context = context;
		}
		@Override
		public void evaluate() {
			callAndStopOnFailure(CreateEmptyAuthzenApiEndpointRequest.class);
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestSubject(subject), "AUTHZEN-5.1");
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestResource(resource), "AUTHZEN-5.2");
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestAction(action), "AUTHZEN-5.3");
			callAndStopOnFailure(new CreateAuthzenApiEndpointRequestContext(context), "AUTHZEN-5.4");

			callAndStopOnFailure(AddSubjectToAuthzenApiEndpointRequest.class, "AUTHZEN-6.1");
			callAndStopOnFailure(AddResourceToAuthzenApiEndpointRequest.class, "AUTHZEN-6.1");
			callAndStopOnFailure(AddActionToAuthzenApiEndpointRequest.class, "AUTHZEN-6.1");
			callAndContinueOnFailure(AddContextToAuthzenApiEndpointRequest.class, "AUTHZEN-6.1");
		}
	}

	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenApiRequestSteps(request.getAsJsonObject("subject"), request.getAsJsonObject("resource"), request.getAsJsonObject("action"), request.getAsJsonObject("context"));
	}

	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(EnsureDecisionResponseTrue.class, ConditionResult.FAILURE);
	}


}
