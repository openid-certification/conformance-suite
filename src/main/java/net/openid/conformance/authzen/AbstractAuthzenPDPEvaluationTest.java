package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenEvaluationApiRequestSteps;
import net.openid.conformance.authzen.condition.EnsureDecisionResponseTrue;
import net.openid.conformance.authzen.condition.EnsureValidDecisionResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointDecisionResponse;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToAccessEvaluationEndpoint;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.ConditionSequence;

public abstract class AbstractAuthzenPDPEvaluationTest extends AbstractAuthzenPDPTest {

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenEvaluationApiRequestSteps(request.getAsJsonObject("subject"), request.getAsJsonObject("resource"), request.getAsJsonObject("action"), request.getAsJsonObject("context"));
	}

	@Override
	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(EnsureDecisionResponseTrue.class, ConditionResult.FAILURE);
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToAccessEvaluationEndpoint.class);
	}

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointDecisionResponse.class, "AUTHZEN-5.5");
		callAndStopOnFailure(EnsureValidDecisionResponse.class, "AUTHZEN-5.5");
	}


}
