package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAndReturnFullResponse;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus401;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromTokenEndpointResponseError;

public abstract class AbstractFAPIRWID2EnsureClientIdInTokenEndpoint extends AbstractFAPIRWID2PerformTokenEndpoint {

	@Override
	protected void createAuthorizationCodeRequest() {

		// Switch to client 2 client
		eventLog.startBlock("Swapping to Client2");
		env.mapKey("client", "client2");

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class, "FAPI-R-5.2.2-19");
	}

	@Override
	protected void requestAuthorizationCode() {
		/* If we get an error back from the token endpoint server:
		 * - It must be a 'invalid_client' error
		 */
		callAndContinueOnFailure(CallTokenEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-19");
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus401.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE,"RFC6749-5.2");

		fireTestFinished();
	}
}
