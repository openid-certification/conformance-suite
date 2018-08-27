package io.fintechlabs.testframework.fapi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddCodeVerifierToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointExpectingError;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForDateHeaderInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForScopesInTokenResponse;
import io.fintechlabs.testframework.condition.client.CheckForSubscriberInIdToken;
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.CreateRandomCodeVerifier;
import io.fintechlabs.testframework.condition.client.CreateRandomFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.CreateS256CodeChallenge;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.DisallowAccessTokenInQuery;
import io.fintechlabs.testframework.condition.client.EnsureMatchingFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.EnsureMinimumTokenEntropy;
import io.fintechlabs.testframework.condition.client.EnsureMinimumTokenLength;
import io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAtHash;
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractCHash;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractImplicitHashToCallbackResponse;
import io.fintechlabs.testframework.condition.client.ExtractSHash;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GenerateJWKsFromClientSecret;
import io.fintechlabs.testframework.condition.client.GenerateResourceEndpointRequestHeaders;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetResourceEndpointConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClient2Configuration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticServerConfiguration;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;
import io.fintechlabs.testframework.condition.client.ValidateAtHash;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

/**
 * @author jricher
 *
 */
@PublishTestModule(
	testName = "fapi-r-code-id-token-with-client-secret-jwt",
	displayName = "FAPI-R: code id_token (client secret jwt authentication)",
	profile = "FAPI-R",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.client_secret",
		"client.client_secret_jwt_alg",
		"client.client_secret_jwt_kid",
		"client2.client_id",
		"client2.client_secret",
		"client2.scope",
		"client2.client_secret_jwt_alg",
		"client2.client_secret_jwt_kid",
		"resource.resourceUrl"
	}
)
public class CodeIdTokenWithClientSecretJWTAssertion extends AbstractTestModule {

	private static final Logger logger = LoggerFactory.getLogger(CodeIdTokenWithClientSecretJWTAssertion.class);

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, java.lang.String)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		callAndContinueOnFailure(GetDynamicServerConfiguration.class);
		callAndContinueOnFailure(GetStaticServerConfiguration.class);

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		callAndStopOnFailure(GenerateJWKsFromClientSecret.class);

		// get the second client and second Key
		callAndStopOnFailure(GetStaticClient2Configuration.class);

		eventLog.startBlock("Loading second client key");
		env.mapKey("client", "client2");
		env.mapKey("client_jwks", "client_jwks2");
		callAndStopOnFailure(GenerateJWKsFromClientSecret.class);
		env.unmapKey("client");
		env.unmapKey("client_jwks");
		eventLog.endBlock();


		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);

		setStatus(Status.CONFIGURED);

		fireSetupDone();

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.startBlock("Authorization endpoint TLS test");
		env.mapKey("tls", "authorization_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		eventLog.startBlock("Token Endpoint TLS test");
		env.mapKey("tls", "token_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		eventLog.startBlock("Userinfo Endpoint TLS test");
		env.mapKey("tls", "userinfo_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		eventLog.startBlock("Registration Endpoint TLS test");
		env.mapKey("tls", "registration_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, ConditionResult.INFO, DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		eventLog.startBlock("Resource Endpoint TLS test");
		env.mapKey("tls", "resource_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		eventLog.endBlock();
		env.unmapKey("tls");

		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);

		call(condition(CreateRandomCodeVerifier.class));
		call(exec().exposeEnvironmentString("code_verifier"));
		call(condition(CreateS256CodeChallenge.class));
		call(exec()
			.exposeEnvironmentString("code_challenge")
			.exposeEnvironmentString("code_challenge_method"));
		call(condition(AddCodeChallengeToAuthorizationEndpointRequest.class)
			.requirement("FAPI-1-5.2.2-7"));

		call(condition(BuildPlainRedirectToAuthorizationEndpoint.class));

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to url", "redirect_to", redirectTo));

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		// dispatch based on the path

		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else if (path.equals(env.getString("implicit_submit", "path"))) {

			if (env.isKeyMapped("client")) {
				// we're doing the second client
				return handleSecondClientImplicitSubmission(requestParts);
			} else {
				// we're doing the first client
				return handleImplicitSubmission(requestParts);
			}

		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}
	}

	@UserFacing
	private ModelAndView handleCallback(JsonObject requestParts) {
		setStatus(Status.RUNNING);

		env.putObject("callback_query_params", requestParts.get("params").getAsJsonObject());

		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CreateRandomImplicitSubmitUrl.class);

		setStatus(Status.WAITING);

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

				logger.info("Hash: " + hash);

				env.putString("implicit_hash", hash);
			} else {
				logger.warn("No hash submitted");

				env.putString("implicit_hash", ""); // Clear any old value
			}

			callAndStopOnFailure(ExtractImplicitHashToCallbackResponse.class);

			callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

			callAndStopOnFailure(CheckMatchingStateParameter.class);

			// check the ID token from the hybrid response

			callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-2-5.2.2-3");

			callAndStopOnFailure(ValidateIdToken.class, "FAPI-2-5.2.2-3");

			callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-2-5.2.2-3");

			callAndStopOnFailure(CheckForSubscriberInIdToken.class, "FAPI-1-5.2.2-24");

			callAndContinueOnFailure(ExtractSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

			skipIfMissing(new String[] { "s_hash" }, null, ConditionResult.INFO,
				ValidateSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

			callAndContinueOnFailure(ExtractCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			skipIfMissing(new String[] { "c_hash" }, null, ConditionResult.INFO,
				ValidateCHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");

			callAndContinueOnFailure(ExtractAtHash.class, ConditionResult.INFO, "OIDCC-3.3.2.11");

			skipIfMissing(new String[] { "at_hash" }, null, ConditionResult.INFO,
				ValidateAtHash.class, ConditionResult.FAILURE, "OIDCC-3.3.2.11");


			// call the token endpoint and complete the flow

			callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

			callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

			callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

			callAndStopOnFailure(SignClientAuthenticationAssertion.class);

			callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);

			call(condition(AddCodeVerifierToTokenEndpointRequest.class));

			callAndStopOnFailure(CallTokenEndpoint.class);

			callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

			callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-1-5.2.2-14");

			callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

			callAndStopOnFailure(CheckForScopesInTokenResponse.class, "FAPI-1-5.2.2-15");

			callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-1-5.2.2-24");

			callAndStopOnFailure(ValidateIdToken.class, "FAPI-1-5.2.2-24");

			callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-1-5.2.2-24");

			callAndStopOnFailure(CheckForSubscriberInIdToken.class, "FAPI-1-5.2.2-24");

			callAndContinueOnFailure(ExtractSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

			skipIfMissing(new String[] { "s_hash" }, null, ConditionResult.INFO,
				ValidateSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

			callAndContinueOnFailure(CheckForRefreshTokenValue.class);

			callAndContinueOnFailure(EnsureMinimumTokenLength.class, ConditionResult.FAILURE, "FAPI-1-5.2.2-16");

			callAndContinueOnFailure(EnsureMinimumTokenEntropy.class, ConditionResult.FAILURE, "FAPI-1-5.2.2-16");

			// verify the access token against a protected resource

			callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
			exposeEnvString("fapi_interaction_id");

			callAndStopOnFailure(GenerateResourceEndpointRequestHeaders.class);

			callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class, "FAPI-1-6.2.2-6");

			callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "FAPI-1-6.2.1-1", "FAPI-1-6.2.1-3");

			callAndStopOnFailure(CheckForDateHeaderInResourceResponse.class, "FAPI-1-6.2.1-11");

			callAndStopOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, "FAPI-1-6.2.1-12");

			callAndContinueOnFailure(EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "FAPI-1-6.2.1-12");

			callAndStopOnFailure(EnsureResourceResponseContentTypeIsJsonUTF8.class, "FAPI-1-6.2.1-9", "FAPI-1-6.2.1-10");

			callAndStopOnFailure(DisallowAccessTokenInQuery.class, "FAPI-1-6.2.1-4");

			// get token for second client
			eventLog.startBlock("Second client");
			env.mapKey("client", "client2");
			env.mapKey("client_jwks", "client_jwks2");

			callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

			callAndStopOnFailure(CreateRandomStateValue.class);
			exposeEnvString("state");
			callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

			callAndStopOnFailure(CreateRandomNonceValue.class);
			exposeEnvString("nonce");
			callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);

			call(condition(CreateRandomCodeVerifier.class));
			call(exec().exposeEnvironmentString("code_verifier"));
			call(condition(CreateS256CodeChallenge.class));
			call(exec()
				.exposeEnvironmentString("code_challenge")
				.exposeEnvironmentString("code_challenge_method"));
			call(condition(AddCodeChallengeToAuthorizationEndpointRequest.class)
				.requirement("FAPI-1-5.2.2-7"));

			call(condition(BuildPlainRedirectToAuthorizationEndpoint.class));

			String redirectTo = env.getString("redirect_to_authorization_endpoint");

			eventLog.log(getName(), args("msg", "Redirecting to url", "redirect_to", redirectTo));

			setStatus(Status.WAITING);

			browser.goToUrl(redirectTo);

			return "done";
		});

		return redirectToLogDetailPage();

	}

	private Object handleSecondClientImplicitSubmission(JsonObject requestParts) {

		getTestExecutionManager().runInBackground(() -> {
			// process the callback
			setStatus(Status.RUNNING);

			JsonElement body = requestParts.get("body");

			if (body != null) {
				String hash = body.getAsString();

				logger.info("Hash: " + hash);

				env.putString("implicit_hash", hash);
			} else {
				logger.warn("No hash submitted");

				env.putString("implicit_hash", ""); // Clear any old value
			}

			callAndStopOnFailure(ExtractImplicitHashToCallbackResponse.class);

			callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

			// we skip the validation steps for the second client and as long as it's not an error we use the results for negative testing

			callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

			callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

			// use the code with the first client's credentials
			env.unmapKey("client");
			env.unmapKey("client_jwks");
			callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

			callAndStopOnFailure(SignClientAuthenticationAssertion.class);

			callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);

			call(condition(AddCodeVerifierToTokenEndpointRequest.class));

			env.mapKey("client", "client2");
			env.mapKey("client_jwks", "client_jwks2");

			callAndStopOnFailure(CallTokenEndpointExpectingError.class);

			// put everything back where we found it
			env.unmapKey("client");
			env.unmapKey("client_jwks");
			eventLog.endBlock();

			fireTestFinished();
			return "done";
		});

		return redirectToLogDetailPage();

	}

}
