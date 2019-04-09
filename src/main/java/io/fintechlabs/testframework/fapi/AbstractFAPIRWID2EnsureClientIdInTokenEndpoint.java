package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;

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
		callAndContinueOnFailure(CallTokenEndpoint.class);

		/* If we get an error back from the token endpoint server:
		 * - It must be a 'invalid_client' error
		 */
		callAndContinueOnFailure(CallTokenEndpointExpectingError.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-19");

		fireTestFinished();
	}
}
