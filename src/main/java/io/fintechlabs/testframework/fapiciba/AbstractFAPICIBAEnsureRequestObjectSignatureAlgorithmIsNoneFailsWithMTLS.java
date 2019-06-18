package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientIdToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddIatToRequestObject;
import io.fintechlabs.testframework.condition.client.AddJtiToRequestObject;
import io.fintechlabs.testframework.condition.client.AddNbfToRequestObject;
import io.fintechlabs.testframework.condition.client.AddRequestToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckBackchannelAuthenticationEndpointHttpStatus400;
import io.fintechlabs.testframework.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.CreateBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.SerializeRequestObjectWithNullAlgorithm;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromBackchannelAuthenticationEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromBackchannelAuthenticationEndpoint;

public abstract class AbstractFAPICIBAEnsureRequestObjectSignatureAlgorithmIsNoneFailsWithMTLS extends AbstractFAPICIBAEnsureSendingInvalidBackchannelAuthorisationRequest {

	@Override
	protected void performAuthorizationRequest() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);
		callAndStopOnFailure(AddIatToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddExpToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddNbfToRequestObject.class, "CIBA-7.1.1");
		callAndStopOnFailure(AddJtiToRequestObject.class, "CIBA-7.1.1");

		// aud, iss are added by SignRequestObject
		callAndStopOnFailure(SerializeRequestObjectWithNullAlgorithm.class, "CIBA-7.2");

		callAndStopOnFailure(CreateBackchannelAuthenticationEndpointRequest.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientIdToBackchannelAuthenticationEndpointRequest.class);
		callAndStopOnFailure(AddRequestToBackchannelAuthenticationEndpointRequest.class);

		callAndStopOnFailure(CallBackchannelAuthenticationEndpoint.class);
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
		//Nothing to do

	}
}
