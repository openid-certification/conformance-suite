package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddAlgorithmAsRS256;
import io.fintechlabs.testframework.condition.client.CheckBackchannelAuthenticationEndpointHttpStatus400;
import io.fintechlabs.testframework.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromBackchannelAuthenticationEndpoint;

public abstract class AbstractFAPICIBAEnsureRequestObjectSignatureAlgorithmIsRS256FailsWithMTLS extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void performAuthorizationRequest() {
		callAndContinueOnFailure(AddAlgorithmAsRS256.class, Condition.ConditionResult.FAILURE, "CIBA-7.2");

		super.performAuthorizationRequest();
	}

	@Override
	protected void performPostAuthorizationResponse() {

		callAndContinueOnFailure(ValidateErrorResponseFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");

		callAndContinueOnFailure(ValidateErrorUriFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");
		callAndContinueOnFailure(ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-13");

		callAndContinueOnFailure(CheckBackchannelAuthenticationEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "CIBA-13");
		callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest.class, Condition.ConditionResult.FAILURE, "CIBA-13");

		fireTestFinished();
	}

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Use client_credentials grant to obtain OpenBanking UK intent_id");

		createAuthorizationRequest();

		performAuthorizationRequest();

		eventLog.endBlock();

		performPostAuthorizationResponse();
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		//Nothings to do

	}
}
