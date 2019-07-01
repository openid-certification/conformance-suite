package io.fintechlabs.testframework.fapi;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddAcrClaimToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddFAPIFinancialIdToResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddIatToRequestObject;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddRedirectUriQuerySuffix;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerTokenExpectingError;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAndReturnFullResponse;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidGrant;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForDateHeaderInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForScopesInTokenResponse;
import io.fintechlabs.testframework.condition.client.CheckForSubjectInIdToken;
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckMatchingCallbackParameters;
import io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus400;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.ConfigurationRequestsTestIsSkipped;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateRandomFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.DisallowAccessTokenInQuery;
import io.fintechlabs.testframework.condition.client.EnsureMatchingFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.EnsureMinimumAccessTokenEntropy;
import io.fintechlabs.testframework.condition.client.EnsureMinimumAccessTokenLength;
import io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAtHash;
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificates2FromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificatesFromConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractSHash;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromOBResourceConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import io.fintechlabs.testframework.condition.client.FAPIGenerateResourceEndpointRequestHeaders;
import io.fintechlabs.testframework.condition.client.FAPIValidateIdTokenSigningAlg;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetResourceEndpointConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClient2Configuration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.RedirectQueryTestDisabled;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.RejectErrorInUrlQuery;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.SetPlainJsonAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.ValidateAtHash;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenACRClaimAgainstRequest;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNonce;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificates2Header;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificatesAsX509;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificatesHeader;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInClientJWKs;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInServerJWKs;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.condition.common.FAPICheckKeyAlgInClientJWKs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFAPIRWID2ServerTestModule extends AbstractRedirectServerTestModule {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected int whichClient;

	protected boolean logEndTestIfAlgIsNotPS256(){return false;}

	@Override
	public final void configure(JsonObject config, String baseUrl, String externalUrlOverride) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		Boolean skip = env.getBoolean("config", "skip_test");
		if (skip != null && skip) {
			// This is intended for use in our CI where we insist all tests run to completion
			// It would be used as a temporary measure in an 'override' where one of the environments we are testing
			// against is not able to run the test to completion due to an issue in that environments.
			callAndContinueOnFailure(ConfigurationRequestsTestIsSkipped.class, Condition.ConditionResult.FAILURE);
			fireTestFinished();
			return;
		}

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

		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);

		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");

		if(logEndTestIfAlgIsNotPS256()){return;}

		// Test won't pass without MATLS, but we'll try anyway (for now)
		callAndContinueOnFailure(ValidateMTLSCertificatesHeader.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, Condition.ConditionResult.FAILURE);

		eventLog.startBlock("Verify configuration of second client");

		// extract second client
		callAndStopOnFailure(GetStaticClient2Configuration.class);
		callAndContinueOnFailure(ValidateMTLSCertificates2Header.class, Condition.ConditionResult.WARNING);
		callAndContinueOnFailure(ExtractMTLSCertificates2FromConfiguration.class, Condition.ConditionResult.FAILURE);

		// get the second client's JWKs
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		callAndStopOnFailure(ExtractJWKsFromStaticClientConfiguration.class);
		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");
		env.unmapKey("client");
		env.unmapKey("client_jwks");

		// validate the secondary MTLS keys
		env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");
		callAndStopOnFailure(ValidateMTLSCertificatesAsX509.class);
		env.unmapKey("mutual_tls_authentication");

		eventLog.endBlock();

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);
		callAndStopOnFailure(ExtractTLSTestValuesFromOBResourceConfiguration.class);

		callAndStopOnFailure(FAPIGenerateResourceEndpointRequestHeaders.class);
		// This header is no longer mentioned in the FAPI standard as of ID2, however the UK OB spec most banks are
		// using (v3.1.1) erroneously requires that this header is sent in all cases, so for now we send it in all cases
		// (even pure FAPI-RW, as it's hard to arrange otherwise).
		callAndStopOnFailure(AddFAPIFinancialIdToResourceEndpointRequest.class);

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

	protected void performAuthorizationFlow() {
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");

		createAuthorizationRequest();

		createAuthorizationRedirect();

		performRedirect();
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
		callAndStopOnFailure(AddAcrClaimToAuthorizationEndpointRequest.class);

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

	protected void onAuthorizationCallbackResponse() {

		callAndStopOnFailure(CheckMatchingCallbackParameters.class);

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		callAndStopOnFailure(CheckMatchingStateParameter.class);

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		performPostAuthorizationFlow();
	}

	protected void performPostAuthorizationFlow() {

		callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-RW-5.2.2-3");

		callAndContinueOnFailure(ValidateIdToken.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-3");

		callAndContinueOnFailure(ValidateIdTokenNonce.class, ConditionResult.FAILURE,"OIDCC-2");

		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1.1");

		performProfileIdTokenValidation();

		callAndContinueOnFailure(ValidateIdTokenSignature.class, ConditionResult.FAILURE, "FAPI-RW-5.2.2-3");

		callAndContinueOnFailure(CheckForSubjectInIdToken.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, ConditionResult.FAILURE, "FAPI-RW-8.6");

		callAndContinueOnFailure(ExtractSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		skipIfMissing(new String[] { "s_hash" }, null, Condition.ConditionResult.INFO,
			ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");

		callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "c_hash" }, null, Condition.ConditionResult.INFO,
			ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		callAndContinueOnFailure(ExtractAtHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");

		skipIfMissing(new String[] { "at_hash" }, null, Condition.ConditionResult.INFO,
			ValidateAtHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

		if (whichClient == 1) {
			// call the token endpoint and complete the flow

			createAuthorizationCodeRequest();

			requestAuthorizationCode();

			eventLog.startBlock("Accounts request endpoint TLS test");
			env.mapKey("tls", "accounts_request_endpoint_tls");
			callAndContinueOnFailure(EnsureTLS12.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
			callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
			callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

			callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-1");
			eventLog.endBlock();


			eventLog.startBlock("Accounts resource endpoint TLS test");
			env.mapKey("tls", "accounts_resource_endpoint_tls");
			callAndContinueOnFailure(EnsureTLS12.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
			callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
			callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

			callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-1");
			env.unmapKey("tls");
			eventLog.endBlock();

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

			requestProtectedResource();

			// Switch back to client 1
			eventLog.startBlock("Try Client1 Crypto Keys with Client2 token");
			env.unmapKey("client");
			env.unmapKey("client_jwks");
			env.unmapKey("mutual_tls_authentication");

			// Try client 2's access token with client 1's keys

			callAndContinueOnFailure(CallAccountsEndpointWithBearerTokenExpectingError.class, Condition.ConditionResult.FAILURE, "OB-6.2.1-2");

			setStatus(Status.WAITING);
			eventLog.endBlock();

			eventLog.startBlock("Attempting reuse of client2's authorisation code & testing if access token is revoked");

			// Re-map to Client 2 keys
			env.mapKey("client", "client2");
			env.mapKey("client_jwks", "client_jwks2");
			env.mapKey("mutual_tls_authentication", "mutual_tls_authentication2");

			generateNewClientAssertion();

			// Check access_token still works
			callAndContinueOnFailure(CallAccountsEndpointWithBearerToken.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");

			callAndContinueOnFailure(CallTokenEndpointAndReturnFullResponse.class, Condition.ConditionResult.WARNING, "FAPI-R-5.2.2-13");
			callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidGrant.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");


			// The AS 'SHOULD' have revoked the access token; try it again".
			callAndContinueOnFailure(CallAccountsEndpointWithBearerTokenExpectingError.class, Condition.ConditionResult.WARNING, "RFC6749-4.1.2");
			eventLog.endBlock();

			fireTestFinished();
		}
	}

	protected void generateNewClientAssertion() {
		// Generate a new client_assertion to test client authentication failure (400 invalid_grant) due to re-use of the authorization code
		// Only use for private_key_jwt
	}

	protected abstract void createAuthorizationCodeRequest();

	protected void requestAuthorizationCode() {

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-R-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		skipIfMissing(new String[] { "expires_in" }, null, Condition.ConditionResult.INFO,
			ValidateExpiresIn.class, Condition.ConditionResult.FAILURE, "RFC6749-5.1");

		callAndContinueOnFailure(CheckForScopesInTokenResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-15");

		callAndContinueOnFailure(CheckForRefreshTokenValue.class);

		callAndContinueOnFailure(EnsureMinimumAccessTokenLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(ValidateIdToken.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(ValidateIdTokenNonce.class, ConditionResult.FAILURE, "OIDCC-2");

		callAndContinueOnFailure(ValidateIdTokenACRClaimAgainstRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-5.5.1.1");

		performProfileIdTokenValidation();

		callAndContinueOnFailure(ValidateIdTokenSignature.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-24");

		callAndContinueOnFailure(CheckForSubjectInIdToken.class, ConditionResult.FAILURE,  "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, ConditionResult.FAILURE, "FAPI-RW-8.6");

		performTokenEndpointIdTokenExtraction();
		callAndContinueOnFailure(ExtractAtHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");

		/* these all use 'INFO' if the field isn't present - whether the hash is a may/should/shall is
		 * determined by the Extract*Hash condition
		 */
		skipIfMissing(new String[] { "c_hash" }, null, Condition.ConditionResult.INFO ,
			ValidateCHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");
		skipIfMissing(new String[] { "s_hash" }, null, Condition.ConditionResult.INFO,
			ValidateSHash.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-4");
		skipIfMissing(new String[] { "at_hash" }, null, Condition.ConditionResult.INFO,
			ValidateAtHash.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.11");

	}

	protected void processCallback() {

		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");

		// FAPI-RW always requires the hybrid flow, use the hash as the response
		env.mapKey("authorization_endpoint_response", "callback_params");

		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");

		onAuthorizationCallbackResponse();

		eventLog.endBlock();
	}

	protected void performProfileIdTokenValidation() {
		// Nothing custom to validate in id_token
	}

	protected void performTokenEndpointIdTokenExtraction() {
		/* code id_token flow - we already had an id_token from the authorisation endpoint,
		 * so c_hash and s_hash are optional.
		 */
		callAndContinueOnFailure(ExtractCHash.class, Condition.ConditionResult.INFO, "OIDCC-3.3.2.11");
		callAndContinueOnFailure(ExtractSHash.class, Condition.ConditionResult.INFO, "FAPI-RW-5.2.2-4");
	}

	protected void requestProtectedResource() {

		// verify the access token against a protected resource
		eventLog.startBlock(currentClientString() + "Resource server endpoint tests");

		if ( whichClient != 2 ) {
			callAndStopOnFailure(FAPIGenerateResourceEndpointRequestHeaders.class);
			// This header is no longer mentioned in the FAPI standard as of ID2, however the UK OB spec most banks are
			// using (v3.1.1) erroneously requires that this header is sent in all cases, so for now we send it in all cases
			// (even pure FAPI-RW, as it's hard to arrange otherwise).
			callAndStopOnFailure(AddFAPIFinancialIdToResourceEndpointRequest.class);
		}

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);

		callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "FAPI-R-6.2.1-1", "FAPI-R-6.2.1-3");

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(EnsureResourceResponseContentTypeIsJsonUTF8.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-9", "FAPI-R-6.2.1-10");

		eventLog.endBlock();
	}

	/** Return which client is in use, for use in block identifiers */
	protected String currentClientString() {
		if (whichClient == 2) {
			return "Second client: ";
		}
		return "";
	}
}
