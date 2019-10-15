package net.openid.conformance.fapi;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenAndCustomHeaders;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerTokenExpectingError;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;

public abstract class AbstractFAPIRWID2AttemptReuseAuthorisationCode extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void performPostAuthorizationFlow() {

		createAuthorizationCodeRequest();

		requestAuthorizationCode();

		checkAccountRequestEndpointTLS();

		checkAccountResourceEndpointTLS();

		requestProtectedResource();

		verifyAccessTokenWithResourceEndpoint();

		eventLog.startBlock("Attempting reuse of authorisation code & testing if access token is revoked");

		// Check access_token still works
		callAndContinueOnFailure(CallProtectedResourceWithBearerTokenAndCustomHeaders.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");

		waitForAmountOfTime();

		callAndContinueOnFailure(CallTokenEndpointAndReturnFullResponse.class, Condition.ConditionResult.WARNING, "FAPI-R-5.2.2-13");

		verifyError();

		// The AS 'SHOULD' have revoked the access token; try it again".
		callAndContinueOnFailure(CallProtectedResourceWithBearerTokenExpectingError.class, Condition.ConditionResult.WARNING, "RFC6749-4.1.2");

		eventLog.endBlock();

		fireTestFinished();
	}

	protected abstract void waitForAmountOfTime();

	protected void verifyError() {
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
	}
}
