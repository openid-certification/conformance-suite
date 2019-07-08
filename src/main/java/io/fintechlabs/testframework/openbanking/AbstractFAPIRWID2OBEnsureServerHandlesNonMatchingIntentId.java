package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CheckStateInAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;

public abstract class AbstractFAPIRWID2OBEnsureServerHandlesNonMatchingIntentId extends AbstractFAPIRWID2OBServerTestModule {

	protected AbstractFAPIRWID2OBEnsureServerHandlesNonMatchingIntentId(StepsConfiguration stepsConfiguration) {
		super(stepsConfiguration);
	}

	@Override
	protected void performAuthorizationFlow() {

		requestClientCredentialsGrant();

		createAccountRequest();

		// Switch to client 2 JWKs
		eventLog.startBlock("Swapping to Client2, Jwks2, tls2");
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

		createAuthorizationRequest();
		createAuthorizationRedirect();

		env.unmapKey("mutual_tls_authentication");
		env.unmapKey("client_jwks");
		env.unmapKey("client");

		eventLog.endBlock();

		performRedirect();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6", "RFC6749-4.2.2.1");
		fireTestFinished();

	}
}
