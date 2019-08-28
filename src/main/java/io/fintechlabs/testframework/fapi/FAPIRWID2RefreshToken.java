package io.fintechlabs.testframework.fapi;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess;
import io.fintechlabs.testframework.condition.client.CheckForSubjectInIdToken;
import io.fintechlabs.testframework.condition.client.CompareIdTokenClaims;
import io.fintechlabs.testframework.condition.client.EnsureRefreshTokenContainsAllowedCharactersOnly;
import io.fintechlabs.testframework.condition.client.ExtractRefreshTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.FAPIValidateIdTokenSigningAlg;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNonce;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignatureUsingKid;
import io.fintechlabs.testframework.sequence.client.RefreshTokenRequestExpectingErrorSteps;
import io.fintechlabs.testframework.sequence.client.RefreshTokenRequestSteps;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-refresh-token",
	displayName = "FAPI-RW-ID2: obtain an id token using a refresh token",
	summary = "This test uses a refresh_token to obtain an id token and ensures that claims satisfy the requirements.",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl",
		"resource.institution_id"
	}
)
public class FAPIRWID2RefreshToken extends AbstractFAPIRWID2ServerTestModule {

	@Variant(name = variant_mtls)
	public void setupMTLS() {
		super.setupMTLS();
	}

	@Variant(name = variant_privatekeyjwt)
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
	}

	@Variant(
		name = variant_openbankinguk_mtls,
		configurationFields = {
			"resource.resourceUrlAccountRequests",
			"resource.resourceUrlAccountsResource",
		}
	)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
	}

	@Variant(
		name = variant_openbankinguk_privatekeyjwt,
		configurationFields = {
			"resource.resourceUrlAccountRequests",
			"resource.resourceUrlAccountsResource",
		}
	)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
	}

	protected void addPromptConsentToAuthorizationEndpointRequest() {
		callAndStopOnFailure(AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess.class, "OIDCC-11");
	}

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		addPromptConsentToAuthorizationEndpointRequest();
	}

	protected boolean sendRefreshTokenRequestAndCheckIdTokenClaims() {
		callAndContinueOnFailure(ExtractRefreshTokenFromTokenResponse.class, Condition.ConditionResult.INFO);
		//stop if no refresh token is returned
		if(Strings.isNullOrEmpty(env.getString("refresh_token"))) {
			fireTestFinished();
			return true;
		}
		callAndContinueOnFailure(EnsureRefreshTokenContainsAllowedCharactersOnly.class, Condition.ConditionResult.FAILURE, "RFC6749-A.17");
		call(new RefreshTokenRequestSteps(isSecondClient(), addTokenEndpointClientAuthentication));

		env.mapKey("first_id_token_claims", "first_id_token.claims");
		env.mapKey("second_id_token_claims", "second_id_token.claims");
		//compare only when refresh response contains an id_token
		call(condition(CompareIdTokenClaims.class)
				.skipIfObjectMissing("second_id_token_claims")
				.requirement("OIDCC-12.2")
				.dontStopOnFailure());
		return false;
	}

	protected void performIdTokenValidation() {
		callAndContinueOnFailure(ValidateIdToken.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-3");

		callAndContinueOnFailure(ValidateIdTokenNonce.class, Condition.ConditionResult.FAILURE,"OIDCC-2");

		performProfileIdTokenValidation();

		callAndContinueOnFailure(ValidateIdTokenSignature.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-3");

		// This condition is a warning because we're not yet 100% sure of the code
		callAndContinueOnFailure(ValidateIdTokenSignatureUsingKid.class, Condition.ConditionResult.WARNING, "FAPI-RW-5.2.2-3");

		callAndContinueOnFailure(CheckForSubjectInIdToken.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");
	}

	@Override
	protected void performPostAuthorizationFlow() {
		if (whichClient == 1) {
			// call the token endpoint and complete the flow

			createAuthorizationCodeRequest();

			env.mapKey("access_token", "first_access_token");
			env.mapKey("id_token", "first_id_token");
			requestAuthorizationCode();

			if(sendRefreshTokenRequestAndCheckIdTokenClaims()) {
				return;
			}

			requestProtectedResource();

			// Try the second client

			//remove refresh token from 1st client
			env.removeNativeValue("refresh_token");

			performAuthorizationFlowWithSecondClient();
		} else {
			// call the token endpoint and complete the flow

			createAuthorizationCodeRequest();

			env.mapKey("access_token", "first_access_token");
			env.mapKey("id_token", "first_id_token");
			requestAuthorizationCode();

			if(sendRefreshTokenRequestAndCheckIdTokenClaims()) {
				return;
			}

			requestProtectedResource();

			switchToClient1AndTryClient2AccessToken();

			// try client 2's refresh_token with client 1
			eventLog.startBlock("Attempting to use refresh_token issued to client 2 with client 1");
			call(new RefreshTokenRequestExpectingErrorSteps(isSecondClient(), addTokenEndpointClientAuthentication));
			eventLog.endBlock();
			fireTestFinished();
		}
	}
}
