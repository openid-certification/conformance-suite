package io.fintechlabs.testframework.openbanking;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddAcrScaClaimToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddIatToRequestObject;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddRedirectUriQuerySuffix;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CallAccountRequestsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerTokenExpectingError;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForDateHeaderInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForScopesInTokenResponse;
import io.fintechlabs.testframework.condition.client.CheckForSubjectInIdToken;
import io.fintechlabs.testframework.condition.client.CheckIfAccountRequestsEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckMatchingCallbackParameters;
import io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateCreateAccountRequestRequest;
import io.fintechlabs.testframework.condition.client.CreateRandomFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.DisallowAccessTokenInQuery;
import io.fintechlabs.testframework.condition.client.EnsureMatchingFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.EnsureMinimumTokenEntropy;
import io.fintechlabs.testframework.condition.client.EnsureMinimumTokenLength;
import io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAccountRequestIdFromAccountRequestsEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractAtHash;
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromClientConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificates2FromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificatesFromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractSHash;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromOBResourceConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import io.fintechlabs.testframework.condition.client.FAPIValidateIdTokenSigningAlg;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GenerateResourceEndpointRequestHeaders;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetResourceEndpointConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClient2Configuration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.OBValidateIdTokenIntentId;
import io.fintechlabs.testframework.condition.client.RedirectQueryTestDisabled;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.SetPlainJsonAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.ValidateAtHash;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenACRClaims;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNonce;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificatesAsX509;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInClientJWKs;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInServerJWKs;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.condition.common.FAPICheckKeyAlgInClientJWKs;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;

public abstract class AbstractOBServerTestModule extends AbstractTestModule {

	private int whichClient;

	enum ResponseMode {
		QUERY,
		FRAGMENT
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, java.lang.String)
	 */
	@Override
	public final void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);
		callAndStopOnFailure(CheckForKeyIdInServerJWKs.class, "OIDCC-10.1");

		whichClient = 1;

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class);

		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, ConditionResult.WARNING, "FAPI-RW-8.6");

		// Test won't pass without MATLS, but we'll try anyway (for now)
		callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, ConditionResult.FAILURE);

		// extract second client
		callAndStopOnFailure(GetStaticClient2Configuration.class);
		callAndContinueOnFailure(ExtractMTLSCertificates2FromConfiguration.class, ConditionResult.FAILURE);

		// get the second client's JWKs
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class);
		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, ConditionResult.WARNING, "FAPI-RW-8.6");
		env.unmapKey("client");
		env.unmapKey("client_jwks");

		// validate the secondary MTLS keys
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
		callAndStopOnFailure(ValidateMTLSCertificatesAsX509.class);
		env.unmapKey("mutual_tls_authentication");

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
		callAndStopOnFailure(ExtractTLSTestValuesFromOBResourceConfiguration.class);

		callAndStopOnFailure(GenerateResourceEndpointRequestHeaders.class);

		// Perform any custom configuration
		onConfigure(config, baseUrl);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	protected void onConfigure(JsonObject config, String baseUrl) {

		// No custom configuration
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {

		setStatus(Status.RUNNING);

		performAuthorizationFlow();
	}

	protected void performPreAuthorizationSteps() {
		/* get an openbanking intent id */
		requestClientCredentialsGrant();

		createAccountRequest();
	}

	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		createAuthorizationRequest();

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo);
	}

	protected void requestClientCredentialsGrant() {

		createClientCredentialsRequest();

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		skipIfMissing(new String[] { "expires_in" }, null, ConditionResult.INFO,
				ValidateExpiresIn.class, ConditionResult.FAILURE, "RFC6749-5.1");
	}

	protected abstract void createClientCredentialsRequest();

	protected abstract ResponseMode getResponseMode();

	protected void createAccountRequest() {

		callAndStopOnFailure(CreateCreateAccountRequestRequest.class);

		callAndStopOnFailure(CallAccountRequestsEndpointWithBearerToken.class);

		callAndStopOnFailure(CheckIfAccountRequestsEndpointResponseError.class);

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, ConditionResult.FAILURE, "FAPI-R-6.2.1-12");

		callAndStopOnFailure(ExtractAccountRequestIdFromAccountRequestsEndpointResponse.class);
	}

	protected void createAuthorizationRequest() {

		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		performProfileAuthorizationEndpointSetup();

		if ( whichClient == 2 ) {
			env.putInteger("requested_state_length", 128);
		} else {
			env.putInteger("requested_state_length", null);
		}

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);
	}

	protected void performProfileAuthorizationEndpointSetup() {
		callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);

		if ( whichClient == 2) {
			callAndStopOnFailure(AddAcrScaClaimToAuthorizationEndpointRequest.class);
		}

	}

	protected void createAuthorizationRedirect() {

		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		if (whichClient == 2) {
			callAndStopOnFailure(AddIatToRequestObject.class);
		}
		callAndStopOnFailure(AddExpToRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	protected Object onAuthorizationCallbackResponse() {

		callAndStopOnFailure(CheckMatchingCallbackParameters.class);

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		callAndStopOnFailure(CheckMatchingStateParameter.class);

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		return performPostAuthorizationFlow();
	}

	protected Object performPostAuthorizationFlow() {

		callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-RW-5.2.2-3");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-RW-5.2.2-3");

		callAndStopOnFailure(ValidateIdTokenNonce.class,"OIDCC-2");

		performProfileIdTokenValidation();

		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-RW-5.2.2-3");

		callAndStopOnFailure(CheckForSubjectInIdToken.class, "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, ConditionResult.WARNING, "FAPI-RW-8.6");

		callAndContinueOnFailure(ExtractSHash.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		skipIfMissing(new String[] { "s_hash" }, null, ConditionResult.INFO,
			ValidateSHash.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		callAndContinueOnFailure(ExtractCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "c_hash" }, null, ConditionResult.INFO,
			ValidateCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		callAndContinueOnFailure(ExtractAtHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "at_hash" }, null, ConditionResult.INFO,
			ValidateAtHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		if (whichClient == 1) {
			setStatus(Status.WAITING);

			getTestExecutionManager().runInBackground(() -> {
				setStatus(Status.RUNNING);
				// call the token endpoint and complete the flow

				createAuthorizationCodeRequest();

				requestAuthorizationCode();

				eventLog.startBlock("Accounts request endpoint TLS test");
				env.mapKey("tls", "accounts_request_endpoint_tls");
				callAndContinueOnFailure(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
				callAndContinueOnFailure(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
				callAndContinueOnFailure(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");

				callAndContinueOnFailure(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-RW-8.5-1");
				eventLog.endBlock();


				eventLog.startBlock("Accounts resource endpoint TLS test");
				env.mapKey("tls", "accounts_resource_endpoint_tls");
				callAndContinueOnFailure(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
				callAndContinueOnFailure(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");
				callAndContinueOnFailure(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-RW-8.5-2");

				callAndContinueOnFailure(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-RW-8.5-1");
				env.unmapKey("tls");
				eventLog.endBlock();

				requestProtectedResource();

				callAndContinueOnFailure(DisallowAccessTokenInQuery.class, ConditionResult.FAILURE, "FAPI-R-6.2.1-4");

				callAndStopOnFailure(SetPlainJsonAcceptHeaderForResourceEndpointRequest.class);

				callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "RFC7231-5.3.2");

				callAndStopOnFailure(SetPermissiveAcceptHeaderForResourceEndpointRequest.class);

				callAndContinueOnFailure(CallAccountsEndpointWithBearerToken.class, ConditionResult.FAILURE, "RFC7231-5.3.2");

				// Try the second client

				whichClient = 2;

				eventLog.startBlock("Second client");
				env.mapKey("client", "client2");
				env.mapKey("client_jwks", "client_jwks2");
				env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

				Integer redirectQueryDisabled = env.getInteger("config", "disableRedirectQueryTest");

				if (redirectQueryDisabled != null && redirectQueryDisabled.intValue() != 0)
				{
					/* Temporary change to allow banks to disable tests until they have had a chance to register new
					 * clients with the new redirect uris.
					 */
					callAndContinueOnFailure(RedirectQueryTestDisabled.class, ConditionResult.FAILURE, "RFC6749-3.1.2");
				}
				else
				{
					callAndStopOnFailure(AddRedirectUriQuerySuffix.class, "RFC6749-3.1.2");
				}
				callAndStopOnFailure(CreateRedirectUri.class, "RFC6749-3.1.2");

				//exposeEnvString("client_id");

				callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class);
				callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
				callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, ConditionResult.WARNING, "FAPI-RW-8.6");

				callAndStopOnFailure(ExtractMTLSCertificates2FromConfiguration.class);
				callAndStopOnFailure(ValidateMTLSCertificatesAsX509.class);

				performAuthorizationFlow();
				return "done";
			});

			return redirectToLogDetailPage();
		} else {
			setStatus(Status.WAITING);

			getTestExecutionManager().runInBackground(() -> {
				setStatus(Status.RUNNING);

				// call the token endpoint and complete the flow

				createAuthorizationCodeRequest();

				requestAuthorizationCode();

				requestProtectedResource();

				// Switch back to client 1
				eventLog.startBlock("Try Client1 Crypto Keys with Client2 token");
				env.unmapKey("client");
				env.unmapKey("client_jwks");
				env.unmapKey("mutual_tls_authentication");

				// Try client 2's access token with client 1's keys

				callAndContinueOnFailure(CallAccountsEndpointWithBearerTokenExpectingError.class, ConditionResult.FAILURE, "OB-6.2.1-2");

				setStatus(Status.WAITING);
				eventLog.endBlock();

				eventLog.startBlock("Attempting reuse of client2's authorisation code & testing if access token is revoked");
				// Re-map to Client 2 keys
				env.mapKey("client", "client2");
				env.mapKey("client_jwks", "client_jwks2");
				env.mapKey("mutual_tls_authentication","mutual_tls_authentication2");

				// Check access_token still works
				callAndContinueOnFailure(CallAccountsEndpointWithBearerToken.class, ConditionResult.FAILURE, "RFC7231-5.3.2");

				callAndContinueOnFailure(CallTokenEndpointExpectingError.class, ConditionResult.WARNING, "FAPI-R-5.2.2-13");

				// The AS 'SHOULD' have revoked the access token; try it again".
				callAndContinueOnFailure(CallAccountsEndpointWithBearerTokenExpectingError.class, ConditionResult.WARNING, "RFC6749-4.1.2");
				eventLog.endBlock();

				fireTestFinished();
				return "done";
			});

			return redirectToLogDetailPage();
		}
	}

	protected abstract void createAuthorizationCodeRequest();

	protected void requestAuthorizationCode() {

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-R-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		skipIfMissing(new String[] { "expires_in" }, null, ConditionResult.INFO,
				ValidateExpiresIn.class, ConditionResult.FAILURE, "RFC6749-5.1");

		callAndContinueOnFailure(CheckForScopesInTokenResponse.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-15");

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);

		callAndContinueOnFailure(EnsureMinimumTokenLength.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndContinueOnFailure(EnsureMinimumTokenEntropy.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(ValidateIdTokenNonce.class,"OIDCC-2");

		performProfileIdTokenValidation();

		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(CheckForSubjectInIdToken.class, "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, ConditionResult.WARNING, "FAPI-RW-8.6");

		performTokenEndpointIdTokenExtraction();
		callAndContinueOnFailure(ExtractAtHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");

		/* these all use 'INFO' if the field isn't present - whether the hash is a may/should/shall is
		 * determined by the Extract*Hash condition
		 */
		skipIfMissing(new String[] { "c_hash" }, null, ConditionResult.INFO ,
			ValidateCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		skipIfMissing(new String[] { "s_hash" }, null, ConditionResult.INFO,
			ValidateSHash.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");
		skipIfMissing(new String[] { "at_hash" }, null, ConditionResult.INFO,
			ValidateAtHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

	}

	protected void performProfileIdTokenValidation() {
		callAndContinueOnFailure(OBValidateIdTokenIntentId.class, ConditionResult.FAILURE, "OIDCC-2");

		if ( whichClient == 2 ) {
			callAndContinueOnFailure(ValidateIdTokenACRClaims.class, ConditionResult.FAILURE, "OIDCC-5.5.1.1");
		}

	}

	protected abstract void performTokenEndpointIdTokenExtraction();

	protected void performTokenEndpointIdTokenExtractionCode() {
		/* code flow, so the only id_token is from the token endpoint, so
		 * c_hash is optional but s_hash is required
		 */
		callAndContinueOnFailure(ExtractCHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");
		callAndContinueOnFailure(ExtractSHash.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");
	}

	protected void performTokenEndpointIdTokenExtractionCodeIdToken() {
		/* code id_token flow - we already had an id_token from the authorisation endpoint,
		 * so c_hash and s_hash are optional.
		 */
		callAndContinueOnFailure(ExtractCHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");
		callAndContinueOnFailure(ExtractSHash.class, ConditionResult.INFO, "FAPI-RW-5.2.2-4");
	}

	protected void requestProtectedResource() {

		// verify the access token against a protected resource

		if ( whichClient != 2 ) {
			callAndStopOnFailure(GenerateResourceEndpointRequestHeaders.class);
		}

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);

		callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "FAPI-R-6.2.1-1", "FAPI-R-6.2.1-3");

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, ConditionResult.FAILURE, "FAPI-R-6.2.1-12");

		callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "FAPI-R-6.2.1-12");

		callAndContinueOnFailure(EnsureResourceResponseContentTypeIsJsonUTF8.class, ConditionResult.FAILURE, "FAPI-R-6.2.1-9", "FAPI-R-6.2.1-10");
	}
}
