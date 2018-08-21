package io.fintechlabs.testframework.fapi;

import java.util.Map;

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
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddCodeVerifierToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
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
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractImplicitHashToTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractSHash;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromResourceConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GenerateResourceEndpointRequestHeaders;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetResourceEndpointConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

/**
 * @author srmoore
 *
 */
@PublishTestModule(
	testName = "fapi-r-code-id-token-with-pkce",
	displayName = "FAPI-R: code id_token (Public Client with PKCE/S256)",
	profile = "FAPI-R",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.fapi_financial_id",
		"tls.testHost",
		"tls.testPort",
		"resource.resourceUrl"
	}
)
public class CodeIdTokenWithPKCE extends AbstractTestModule {

	public static Logger logger = LoggerFactory.getLogger(CodeIdTokenWithPKCE.class);

	/**
	 *
	 */
	public CodeIdTokenWithPKCE(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager) {
		super(id, owner, eventLog, browser, testInfo, executionManager);
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#configure(com.google.gson.JsonObject)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.putObject("config", config);

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		callAndStopOnFailure(GetDynamicServerConfiguration.class);

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		callAndStopOnFailure(ExtractTLSTestValuesFromResourceConfiguration.class);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#start()
	 */
	@Override
	public void start() {

		setStatus(Status.RUNNING);

		eventLog.startBlock("Authorization endpoint TLS test");
		env.mapKey("tls", "authorization_endpoint_tls");
		call(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		eventLog.startBlock("Token Endpoint TLS test");
		env.mapKey("tls", "token_endpoint_tls");
		call(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

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
		call(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		eventLog.endBlock();
		env.unmapKey("tls");

		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		call(condition(CreateRandomCodeVerifier.class));
		call(exec().exposeEnvironmentString("code_verifier"));
		call(condition(CreateS256CodeChallenge.class));
		call(exec()
			.exposeEnvironmentString("code_challenge")
			.exposeEnvironmentString("code_challenge_method"));
		call(condition(AddCodeChallengeToAuthorizationEndpointRequest.class)
			.requirement("FAPI-1-5.2.2-7"));

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);

		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint",
			"redirect_to", redirectTo,
			"http", "redirect"));

		setStatus(Status.WAITING);

		browser.goToUrl(redirectTo);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, org.springframework.util.MultiValueMap, org.springframework.ui.Model)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		// dispatch based on the path

		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else if (path.equals(env.getString("implicit_submit", "path"))) {
			return handleImplicitSubmission(requestParts);
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}

	}

	@UserFacing
	private ModelAndView handleCallback(JsonObject requestParts) {
		setStatus(Status.RUNNING);

		callAndStopOnFailure(CreateRandomImplicitSubmitUrl.class);

		setStatus(Status.WAITING);

		return new ModelAndView("implicitCallback",
			ImmutableMap.of(
				"implicitSubmitUrl", env.getString("implicit_submit", "fullUrl"),
				"returnUrl", "/log-detail.html?log=" + getId()
			));
	}

	/**
	 * @param path
	 * @param req
	 * @param res
	 * @param session
	 * @param params
	 * @param m
	 * @return
	 */
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

			callAndStopOnFailure(ExtractImplicitHashToTokenEndpointResponse.class);

			callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

			callAndStopOnFailure(CheckMatchingStateParameter.class);

			callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

			callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

			callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);

			callAndStopOnFailure(AddCodeVerifierToTokenEndpointRequest.class);

			callAndStopOnFailure(CallTokenEndpoint.class);

			callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

			callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-1-5.2.2-14");

			callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

			callAndStopOnFailure(CheckForScopesInTokenResponse.class, "FAPI-1-5.2.2-15");

			callAndStopOnFailure(ExtractIdTokenFromTokenResponse.class, "FAPI-1-5.2.2-24");

			callAndStopOnFailure(ValidateIdToken.class, "FAPI-1-5.2.2-24");

			callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-1-5.2.2-24");

			callAndStopOnFailure(CheckForSubscriberInIdToken.class, "FAPI-1-5.2.2-24");

			call(ExtractSHash.class);

			skipIfMissing(new String[] { "s_hash" }, null, ConditionResult.INFO,
				ValidateSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

			call(CheckForRefreshTokenValue.class);

			call(EnsureMinimumTokenLength.class, ConditionResult.FAILURE, "FAPI-1-5.2.2-16");

			call(EnsureMinimumTokenEntropy.class, "FAPI-1-5.2.2-16");

			// verify the access token against a protected resource

			callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
			exposeEnvString("fapi_interaction_id");

			callAndStopOnFailure(GenerateResourceEndpointRequestHeaders.class);

			callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);

			callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "FAPI-1-6.2.1-1", "FAPI-1-6.2.1-3");

			callAndStopOnFailure(CheckForDateHeaderInResourceResponse.class, "FAPI-1-6.2.1-11");

			callAndStopOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, "FAPI-1-6.2.1-12");

			call(EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "FAPI-1-6.2.1-12");

			callAndStopOnFailure(EnsureResourceResponseContentTypeIsJsonUTF8.class, "FAPI-1-6.2.1-9", "FAPI-1-6.2.1-10");

			callAndStopOnFailure(DisallowAccessTokenInQuery.class, "FAPI-1-6.2.1-4");

			fireTestFinished();
			return "done";
		});

		return redirectToLogDetailPage();

	}

}
