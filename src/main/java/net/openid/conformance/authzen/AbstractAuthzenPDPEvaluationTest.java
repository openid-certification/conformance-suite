package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenEvaluationApiRequestSteps;
import net.openid.conformance.authzen.condition.EnsureDecisionResponseTrue;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.ConditionSequence;

public abstract class AbstractAuthzenPDPEvaluationTest extends AbstractAuthzenPDPTest {

	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenEvaluationApiRequestSteps(request.getAsJsonObject("subject"), request.getAsJsonObject("resource"), request.getAsJsonObject("action"), request.getAsJsonObject("context"));
	}

	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(EnsureDecisionResponseTrue.class, ConditionResult.FAILURE);
	}


}
