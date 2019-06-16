package io.fintechlabs.testframework.fapi;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddPromptConsentToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddRedirectUriQuerySuffix;
import io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerTokenExpectingError;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAndReturnFullResponse;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import io.fintechlabs.testframework.condition.client.CheckForSubjectInIdToken;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus400;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.CompareIdTokenClaims;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.CreateRefreshTokenRequest;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.DisallowAccessTokenInQuery;
import io.fintechlabs.testframework.condition.client.EnsureAccessTokenContainsAllowedCharactersOnly;
import io.fintechlabs.testframework.condition.client.EnsureAccessTokenValuesAreDifferent;
import io.fintechlabs.testframework.condition.client.EnsureMinimumTokenEntropy;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAtHash;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificates2FromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractRefreshTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractSHash;
import io.fintechlabs.testframework.condition.client.FAPIValidateIdTokenSigningAlg;
import io.fintechlabs.testframework.condition.client.RedirectQueryTestDisabled;
import io.fintechlabs.testframework.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.SetPlainJsonAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.ValidateAtHash;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateErrorFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNonce;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificates2Header;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificatesAsX509;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.condition.client.WaitForOneSecond;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInClientJWKs;
import io.fintechlabs.testframework.condition.common.FAPICheckKeyAlgInClientJWKs;

public abstract class AbstractFAPIRWID2RefreshTokenTestModule extends AbstractFAPIRWID2ServerTestModule {

	protected abstract void addClientAuthenticationToTokenEndpointRequest();

	@Override
	protected void createAuthorizationCodeRequest() {
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		addClientAuthenticationToTokenEndpointRequest();
	}

	protected void addPromptConsentToAuthorizationEndpointRequest() {
		callAndStopOnFailure(AddPromptConsentToAuthorizationEndpointRequest.class, "OIDCC-11");
	}

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		addPromptConsentToAuthorizationEndpointRequest();
	}

	protected void sendRefreshTokenRequestAndCheckIdTokenClaims() {
		addIdTokenClaimsToEnv("first_id_token_claims");
		callAndStopOnFailure(ExtractRefreshTokenFromTokenResponse.class, Condition.ConditionResult.INFO);
		//stop if no refresh token is returned
		if(env.getString("refresh_token") == null) {
			fireTestFinished();
		}
		refreshTokenRequest();
		//compare only when refresh response contains an id_token
		if(addIdTokenClaimsToEnv("second_id_token_claims")) {
			callAndContinueOnFailure(CompareIdTokenClaims.class, Condition.ConditionResult.FAILURE, "OIDCC-12.2");
		}
	}

	protected boolean addIdTokenClaimsToEnv(String targetKey) {
		JsonObject idToken = env.getObject("id_token");
		if(idToken!=null) {
			env.putObject(targetKey, idToken.get("claims").getAsJsonObject());
			return true;
		}
		return false;
	}

	protected void refreshTokenRequest() {
		eventLog.startBlock(currentClientString() + "Refresh Token Request");
		env.putString("scope", env.getString("client", "scope"));
		callAndStopOnFailure(CreateRefreshTokenRequest.class);

		addClientAuthenticationToTokenEndpointRequest();

		//wait 1 second to make sure that iat values will be different
		callAndStopOnFailure(WaitForOneSecond.class);

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
		callAndContinueOnFailure(EnsureMinimumTokenEntropy.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");
		callAndContinueOnFailure(EnsureAccessTokenContainsAllowedCharactersOnly.class, Condition.ConditionResult.FAILURE, "RFC6749-A.12");

		String secondAccessToken = env.getString("token_endpoint_response", "access_token");
		env.putString("second_access_token", secondAccessToken);

		callAndContinueOnFailure(EnsureAccessTokenValuesAreDifferent.class);

		env.removeObject("id_token");
		callAndContinueOnFailure(ExtractIdTokenFromTokenResponse.class);

		env.removeObject("refresh_token");
		callAndStopOnFailure(ExtractRefreshTokenFromTokenResponse.class, Condition.ConditionResult.INFO);

		eventLog.endBlock();
	}

	protected void refreshTokenRequestExpectingError() {
		env.putString("scope", env.getString("client", "scope"));
		callAndStopOnFailure(CreateRefreshTokenRequest.class);

		addClientAuthenticationToTokenEndpointRequest();

		callAndContinueOnFailure(CallTokenEndpointAndReturnFullResponse.class);
		callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class);
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
	}

	protected void performStandardIdTokenValidation() {
		callAndContinueOnFailure(ValidateIdTokenSignature.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-3");

		callAndContinueOnFailure(CheckForSubjectInIdToken.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");

		callAndContinueOnFailure(ExtractSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		skipIfMissing(new String[] { "s_hash" }, null, Condition.ConditionResult.INFO,
			ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "c_hash" }, null, Condition.ConditionResult.INFO,
			ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		callAndContinueOnFailure(ExtractAtHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "at_hash" }, null, Condition.ConditionResult.INFO,
			ValidateAtHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
	}

	@Override
	protected void performPostAuthorizationFlow() {
		callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-RW-5.2.2-3");
		addIdTokenClaimsToEnv("first_id_token_claims");

		callAndContinueOnFailure(ValidateIdToken.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-3");

		callAndContinueOnFailure(ValidateIdTokenNonce.class, Condition.ConditionResult.FAILURE,"OIDCC-2");

		performProfileIdTokenValidation();

		performStandardIdTokenValidation();

		if (whichClient == 1) {
			// call the token endpoint and complete the flow

			createAuthorizationCodeRequest();

			requestAuthorizationCode();
			String firstAccessToken = env.getString("token_endpoint_response", "access_token");
			env.putString("first_access_token", firstAccessToken);

			sendRefreshTokenRequestAndCheckIdTokenClaims();

			requestProtectedResource();

			callAndContinueOnFailure(DisallowAccessTokenInQuery.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-4");

			callAndStopOnFailure(SetPlainJsonAcceptHeaderForResourceEndpointRequest.class);

			callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "RFC7231-5.3.2");

			callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);

			callAndContinueOnFailure(CallAccountsEndpointWithBearerToken.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");

			// Try the second client

			whichClient = 2;

			eventLog.startBlock(currentClientString() + "Setup");
			env.mapKey("client", "client2");
			env.mapKey("client_jwks", "client_jwks2");
			env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

			Integer redirectQueryDisabled = env.getInteger("config", "disableRedirectQueryTest");

			if (redirectQueryDisabled != null && redirectQueryDisabled.intValue() != 0) {
				/* Temporary change to allow banks to disable tests until they have had a chance to register new
				 * clients with the new redirect uris.
				 */
				callAndContinueOnFailure(RedirectQueryTestDisabled.class, Condition.ConditionResult.FAILURE, "RFC6749-3.1.2");
			} else {
				callAndStopOnFailure(AddRedirectUriQuerySuffix.class, "RFC6749-3.1.2");
			}
			callAndStopOnFailure(CreateRedirectUri.class, "RFC6749-3.1.2");

			//exposeEnvString("client_id");

			callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
			callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
			callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");

			callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(ExtractMTLSCertificates2FromConfiguration.class);
			callAndStopOnFailure(ValidateMTLSCertificatesAsX509.class);

			performAuthorizationFlow();
		} else {
			// call the token endpoint and complete the flow

			createAuthorizationCodeRequest();

			requestAuthorizationCode();
			String firstAccessToken = env.getString("token_endpoint_response", "access_token");
			env.putString("first_access_token", firstAccessToken);

			sendRefreshTokenRequestAndCheckIdTokenClaims();

			requestProtectedResource();

			// Switch back to client 1
			////eventLog.startBlock("Try Client1 Crypto Keys with Client2 token");
			env.unmapKey("client");
			env.unmapKey("client_jwks");
			env.unmapKey("mutual_tls_authentication");

			// Try client 2's access token with client 1's keys

			callAndContinueOnFailure(CallAccountsEndpointWithBearerTokenExpectingError.class, Condition.ConditionResult.FAILURE, "OB-6.2.1-2");

			setStatus(Status.WAITING);
			eventLog.endBlock();


			// try client 2's refresh_token with client 1
			eventLog.startBlock("Attempting to use refresh_token issued to client 2 with client 1");
			refreshTokenRequestExpectingError();
			eventLog.endBlock();
			fireTestFinished();
		}
	}
}
