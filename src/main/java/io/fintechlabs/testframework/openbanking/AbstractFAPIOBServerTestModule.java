package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddAcrScaClaimToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallAccountRequestsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckIfAccountRequestsEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CreateCreateAccountRequestRequest;
import io.fintechlabs.testframework.condition.client.CreateCreateAccountRequestRequestWithExpiration;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAccountRequestIdFromAccountRequestsEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.OBValidateIdTokenIntentId;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenACRClaims;
import io.fintechlabs.testframework.fapi.AbstractFAPIRWServerTestModule;

public abstract class AbstractFAPIOBServerTestModule extends AbstractFAPIRWServerTestModule {

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		super.performAuthorizationFlow();
	}

	protected void performPreAuthorizationSteps() {
		/* get an openbanking intent id */
		requestClientCredentialsGrant();

		createAccountRequest();
	}

	@Override
	protected void performProfileIdTokenValidation() {
		callAndContinueOnFailure(OBValidateIdTokenIntentId.class, Condition.ConditionResult.FAILURE, "OIDCC-2");

		if ( whichClient == 2 ) {
			callAndContinueOnFailure(ValidateIdTokenACRClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1.1");
		}

	}

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);

		if ( whichClient == 2) {
			callAndStopOnFailure(AddAcrScaClaimToAuthorizationEndpointRequest.class);
		}

	}

	protected abstract void createClientCredentialsRequest();

	protected void requestClientCredentialsGrant() {

		createClientCredentialsRequest();

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");
	}

	protected void createAccountRequest() {

		if ( whichClient == 2 ) {
			callAndStopOnFailure(CreateCreateAccountRequestRequestWithExpiration.class);

		} else {
			callAndStopOnFailure(CreateCreateAccountRequestRequest.class);
		}

		callAndStopOnFailure(CallAccountRequestsEndpointWithBearerToken.class);

		callAndStopOnFailure(CheckIfAccountRequestsEndpointResponseError.class);

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-12");

		callAndStopOnFailure(ExtractAccountRequestIdFromAccountRequestsEndpointResponse.class);
	}

}
