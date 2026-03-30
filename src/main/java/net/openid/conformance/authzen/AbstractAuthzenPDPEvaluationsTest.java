package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CreateAuthzenEvaluationsApiRequestSteps;
import net.openid.conformance.authzen.condition.EnsureAuthzenEvaluationsResponseElemNumbersMatchRequest;
import net.openid.conformance.authzen.condition.EnsureAuthzenEvaluationsResponseValsMatchExpectedVals;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointEvaluationsResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenEvaluationsExpectedResponse;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToAccessEvaluationsEndpoint;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.ConditionSequence;

public abstract class AbstractAuthzenPDPEvaluationsTest extends AbstractAuthzenPDPTest {

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenEvaluationsApiRequestSteps(
			request.getAsJsonObject("subject"),
			request.getAsJsonObject("resource"),
			request.getAsJsonObject("action"),
			request.getAsJsonObject("context"),
			request.getAsJsonArray("evaluations"),
			request.getAsJsonObject("options"));
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToAccessEvaluationsEndpoint.class);
	}

	protected abstract String getExpectedEvaluationsResponseJson();

	@Override
	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(new ExtractAuthzenEvaluationsExpectedResponse(getExpectedEvaluationsResponseJson()), ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureAuthzenEvaluationsResponseElemNumbersMatchRequest.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureAuthzenEvaluationsResponseValsMatchExpectedVals.class, ConditionResult.FAILURE);
	}

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointEvaluationsResponse.class, "AUTHZEN-7.2");
	}

}
