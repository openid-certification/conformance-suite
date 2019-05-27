package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallAccountRequestsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForSubjectInIdToken;
import io.fintechlabs.testframework.condition.client.CheckIfAccountRequestsEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CreateCreateAccountRequestRequest;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAccountRequestIdFromAccountRequestsEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractAtHash;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificates2FromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractSHash;
import io.fintechlabs.testframework.condition.client.OBValidateIdTokenIntentId;
import io.fintechlabs.testframework.condition.client.ValidateAtHash;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNonce;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.fapi.AbstractFAPIRWID2EnsureAuthorizationCodeIsBoundToClient;

public abstract class AbstractFAPIRWID2OBEnsureAuthorizationCodeIsBoundToClient extends AbstractFAPIRWID2EnsureAuthorizationCodeIsBoundToClient {

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		super.performAuthorizationFlow();
	}

	protected void performPreAuthorizationSteps() {
		eventLog.startBlock(currentClientString() + "Use client_credentials grant to obtain OpenBanking UK intent_id");

		/* get an openbanking intent id */
		requestClientCredentialsGrant();

		createAccountRequest();

		eventLog.endBlock();
	}

	@Override
	protected void performProfileIdTokenValidation() {
		callAndContinueOnFailure(OBValidateIdTokenIntentId.class, Condition.ConditionResult.FAILURE, "OIDCC-2");

	}

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);

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

		callAndStopOnFailure(CreateCreateAccountRequestRequest.class);

		callAndStopOnFailure(CallAccountRequestsEndpointWithBearerToken.class);

		callAndStopOnFailure(CheckIfAccountRequestsEndpointResponseError.class);

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-12");

		callAndStopOnFailure(ExtractAccountRequestIdFromAccountRequestsEndpointResponse.class);
	}
}
