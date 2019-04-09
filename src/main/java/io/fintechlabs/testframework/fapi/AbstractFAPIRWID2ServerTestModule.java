package io.fintechlabs.testframework.fapi;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddAcrClaimToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddIatToRequestObject;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddRedirectUriQuerySuffix;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
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
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckMatchingCallbackParameters;
import io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.client.ConfigurationRequestsTestIsSkipped;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
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
import io.fintechlabs.testframework.condition.client.ExtractAtHash;
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractImplicitHashToCallbackResponse;
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
import io.fintechlabs.testframework.condition.client.RedirectQueryTestDisabled;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.RejectErrorInUrlQuery;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.client.SetPermissiveAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.SetPlainJsonAcceptHeaderForResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.TestCanOnlyBePerformedForPS256Alg;
import io.fintechlabs.testframework.condition.client.ValidateAtHash;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenNonce;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateMTLSCertificatesAsX509;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInClientJWKs;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInServerJWKs;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.condition.common.FAPICheckKeyAlgInClientJWKs;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.UserFacing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public abstract class AbstractFAPIRWID2ServerTestModule extends AbstractTestModule {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected int whichClient;

	@Override
	public final void configure(JsonObject config, String baseUrl) {
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

		callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class);

		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");

		// ES256 keys are supplied, but we can't do this and the test module should probably just immediately exit successfully
		// We don't need to check null for jwks and keys because it was checked the steps before
		// We get first key to compare with PS256 because we use it to sign request_object or client_assertion
		JsonObject jwks = env.getObject("client_jwks");
		JsonArray keys = jwks.get("keys").getAsJsonArray();
		JsonObject key = keys.get(0).getAsJsonObject();
		String alg = key.get("alg").getAsString();
		if (!alg.equals("PS256")) {
			callAndContinueOnFailure(TestCanOnlyBePerformedForPS256Alg.class, Condition.ConditionResult.FAILURE);
			fireTestFinished();
			return;

		}

		// Test won't pass without MATLS, but we'll try anyway (for now)
		callAndContinueOnFailure(ExtractMTLSCertificatesFromConfiguration.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateMTLSCertificatesAsX509.class, Condition.ConditionResult.FAILURE);

		// extract second client
		callAndStopOnFailure(GetStaticClient2Configuration.class);
		callAndContinueOnFailure(ExtractMTLSCertificates2FromConfiguration.class, Condition.ConditionResult.FAILURE);

		// get the second client's JWKs
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class);
		callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
		callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");
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

	protected void performAuthorizationFlow() {

		createAuthorizationRequest();

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo);
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

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-RW-5.2.2-3");

		callAndStopOnFailure(ValidateIdTokenNonce.class,"OIDCC-2");

		performProfileIdTokenValidation();

		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-RW-5.2.2-3");

		callAndStopOnFailure(CheckForSubjectInIdToken.class, "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, Condition.ConditionResult.WARNING, "FAPI-RW-8.6");

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

			eventLog.startBlock("Second client");
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

			callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class);
			callAndStopOnFailure(CheckForKeyIdInClientJWKs.class, "OIDCC-10.1");
			callAndContinueOnFailure(FAPICheckKeyAlgInClientJWKs.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.6");

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

			// Check access_token still works
			callAndContinueOnFailure(CallAccountsEndpointWithBearerToken.class, Condition.ConditionResult.FAILURE, "RFC7231-5.3.2");

			callAndContinueOnFailure(CallTokenEndpointExpectingError.class, Condition.ConditionResult.WARNING, "FAPI-R-5.2.2-13");

			// The AS 'SHOULD' have revoked the access token; try it again".
			callAndContinueOnFailure(CallAccountsEndpointWithBearerTokenExpectingError.class, Condition.ConditionResult.WARNING, "RFC6749-4.1.2");
			eventLog.endBlock();

			fireTestFinished();
		}
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

		callAndContinueOnFailure(EnsureMinimumTokenLength.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndContinueOnFailure(EnsureMinimumTokenEntropy.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-16");

		callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(ValidateIdTokenNonce.class,"OIDCC-2");

		performProfileIdTokenValidation();

		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-R-5.2.2-24");

		callAndStopOnFailure(CheckForSubjectInIdToken.class, "FAPI-R-5.2.2-24", "OB-5.2.2-8");
		callAndContinueOnFailure(FAPIValidateIdTokenSigningAlg.class, Condition.ConditionResult.WARNING, "FAPI-RW-8.6");

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

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else if (path.equals(env.getString("implicit_submit", "path"))) {
			return handleImplicitSubmission(requestParts);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}

	}

	@UserFacing
	private Object handleCallback(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.putObject("callback_query_params", requestParts.get("params").getAsJsonObject());

		callAndStopOnFailure(CreateRandomImplicitSubmitUrl.class);

		setStatus(Status.WAITING);

		String submissionUrl = env.getString("implicit_submit", "fullUrl");
		logger.info("Sending JS to user's browser to submit URL fragment (hash) to " + submissionUrl);

		return new ModelAndView("implicitCallback",
			ImmutableMap.of(
				"implicitSubmitUrl", env.getString("implicit_submit", "fullUrl"),
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

	private Object handleImplicitSubmission(JsonObject requestParts) {

		getTestExecutionManager().runInBackground(() -> {

			// process the callback
			setStatus(Status.RUNNING);

			JsonElement body = requestParts.get("body");

			if (body != null) {
				String hash = body.getAsString();

				logger.info("URL fragment (hash): " + hash);

				env.putString("implicit_hash", hash);
			} else {
				logger.warn("No hash/URL fragment submitted");

				env.putString("implicit_hash", ""); // Clear any old value
			}

			callAndStopOnFailure(ExtractImplicitHashToCallbackResponse.class);

			// always the hybrid flow for OB, use the hash as the response
			env.mapKey("authorization_endpoint_response", "callback_params");

			callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

			callAndContinueOnFailure(RejectErrorInUrlQuery.class, Condition.ConditionResult.FAILURE, "OAuth2-RT-5");

			onAuthorizationCallbackResponse();

			return "done";
		});

		return redirectToLogDetailPage();

	}

	protected void performProfileIdTokenValidation() {

		// No custom to validate profile_id_token
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

		if ( whichClient != 2 ) {
			callAndStopOnFailure(GenerateResourceEndpointRequestHeaders.class);
		}

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);

		callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "FAPI-R-6.2.1-1", "FAPI-R-6.2.1-3");

		callAndContinueOnFailure(CheckForDateHeaderInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11");

		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-12");

		callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-12");

		callAndContinueOnFailure(EnsureResourceResponseContentTypeIsJsonUTF8.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-9", "FAPI-R-6.2.1-10");
	}
}
