package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest;
import org.apache.http.HttpStatus;

// Send invalid request to backchannel authorisation endpoint and the response is invalid_request
public abstract class AbstractFAPICIBAEnsureSendingInvalidBackchannelAuthorisationRequestWithMTLS extends AbstractFAPICIBAWithMTLS {
	protected abstract void cleanupAfterBackchannelRequestShouldHaveFailed();

	protected void pollCleanupAfterBackchannelRequestShouldHaveFailed() {
		// no cleanup necessary, just finish
		fireTestFinished();
	}

	protected void pingCleanupAfterBackchannelRequestShouldHaveFailed() {
		Integer httpStatus = env.getInteger("backchannel_authentication_endpoint_response_http_status");
		if (httpStatus != HttpStatus.SC_OK) {
			// error as expected, go on and complete test as normal
			fireTestFinished();
		} else {
			// no error - we don't want to leave a authorization request in progress (as it would result in a ping
			// notification arriving later, potentially when the user has started another test, which would be
			// confusing - complete the process
			callAutomatedEndpoint();
		}
	}

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		eventLog.startBlock(currentClientString() + "Call backchannel authentication endpoint");

		createAuthorizationRequest();

		performAuthorizationRequest();

		eventLog.endBlock();

		validateErrorFromBackchannelAuthorizationRequestResponse();

		callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest.class, Condition.ConditionResult.FAILURE, "CIBA-13");

		cleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		//Not called in this test
	}

	protected void performPostAuthorizationFlow() {
		// we shouldn't get here anyway, but if we do, just check access token, don't go on and try second client
		requestProtectedResource();
		fireTestFinished();
	}

}
