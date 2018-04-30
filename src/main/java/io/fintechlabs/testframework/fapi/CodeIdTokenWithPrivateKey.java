/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

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
import io.fintechlabs.testframework.condition.client.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
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
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.client.CreateRandomFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
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
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromClientConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractSHash;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetResourceEndpointConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticServerConfiguration;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.client.SetTLSTestHostToResourceEndpoint;
import io.fintechlabs.testframework.condition.client.SignClientAuthenticationAssertion;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.ValidateAtHash;
import io.fintechlabs.testframework.condition.client.ValidateCHash;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateSHash;
import io.fintechlabs.testframework.condition.common.CheckForKeyIdInJWKs;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;
import io.fintechlabs.testframework.condition.common.SetTLSTestHostFromConfig;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

/**
 * @author jricher
 *
 */
@PublishTestModule(
	testName = "code-id-token-with-private-key",
	displayName = "code id_token (private key authentication)",
	profile = "FAPI",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"resource.resourceUrl"
	}
)
public class CodeIdTokenWithPrivateKey extends AbstractTestModule {

	private static final Logger logger = LoggerFactory.getLogger(CodeIdTokenWithPrivateKey.class);

	/**
	 * @param name
	 */
	public CodeIdTokenWithPrivateKey(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, java.lang.String)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.put("config", config);

		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		call(GetDynamicServerConfiguration.class);
		call(GetStaticServerConfiguration.class);

		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(FetchServerKeys.class);

		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);

		exposeEnvString("client_id");

		callAndStopOnFailure(ExtractJWKsFromClientConfiguration.class);

		callAndStopOnFailure(CheckForKeyIdInJWKs.class, "OIDCC-10.1");

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);

		setStatus(Status.CONFIGURED);

		fireSetupDone();

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);

		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), "Redirecting to url " + redirectTo);

		browser.goToUrl(redirectTo);

		setStatus(Status.WAITING);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		logIncomingHttpRequest(path, requestParts);

		// dispatch based on the path

		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else if (path.equals(env.getString("implicit_submit", "path"))) {
			return handleImplicitSubmission(requestParts);
		} else {
			return new ModelAndView("testError");
		}
	}

	@UserFacing
	private ModelAndView handleCallback(JsonObject requestParts) {
		setStatus(Status.RUNNING);

		env.put("callback_query_params", requestParts.get("params").getAsJsonObject());

		call(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		callAndStopOnFailure(CreateRandomImplicitSubmitUrl.class);

		setStatus(Status.WAITING);

		return new ModelAndView("implicitCallback",
			ImmutableMap.of("test", this,
				"implicitSubmitUrl", env.getString("implicit_submit", "fullUrl")));
	}

	private ModelAndView handleImplicitSubmission(JsonObject requestParts) {

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

		callAndStopOnFailure(ExtractIdTokenFromAuthorizationResponse.class, "FAPI-2-5.2.2-3");

		callAndStopOnFailure(ValidateIdToken.class, "FAPI-2-5.2.2-3");

		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-2-5.2.2-3");

		callAndStopOnFailure(CheckForSubscriberInIdToken.class, "FAPI-1-5.2.2-24");

		call(ExtractSHash.class, "FAPI-2-5.2.2-4");

		skipIfMissing(new String[] { "state_hash" }, new String[] {}, ConditionResult.INFO,
			ValidateSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");
		
		call(ExtractCHash.class, "FAPI-2-5.2.2-4");

		skipIfMissing(new String[] { "c_hash" }, new String[] {}, ConditionResult.INFO,
			ValidateCHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");
		
		call(ExtractAtHash.class, "FAPI-2-5.2.2-4");

		skipIfMissing(new String[] { "at_hash" }, new String[] {}, ConditionResult.INFO,
			ValidateAtHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");		
		
		// check the ID token from the hybrid response

		// call the token endpoint and complete the flow

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		callAndStopOnFailure(CreateClientAuthenticationAssertionClaims.class);

		callAndStopOnFailure(SignClientAuthenticationAssertion.class);

		callAndStopOnFailure(AddClientAssertionToTokenEndpointRequest.class);

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

		skipIfMissing(new String[] { "state_hash" }, new String[] {}, ConditionResult.INFO,
			ValidateSHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

		call(CheckForRefreshTokenValue.class);

		call(EnsureMinimumTokenLength.class, ConditionResult.FAILURE, "FAPI-1-5.2.2-16");

		call(EnsureMinimumTokenEntropy.class, "FAPI-1-5.2.2-16");

		// verify the access token against a protected resource

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		callAndStopOnFailure(SetTLSTestHostFromConfig.class);
		call(EnsureTLS12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");

		callAndStopOnFailure(SetTLSTestHostToResourceEndpoint.class);
		call(DisallowInsecureCipher.class, ConditionResult.FAILURE, "FAPI-2-8.5-1");

		callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "FAPI-1-6.2.1-1", "FAPI-1-6.2.1-3");

		callAndStopOnFailure(DisallowAccessTokenInQuery.class, "FAPI-1-6.2.1-4");

		callAndStopOnFailure(CheckForDateHeaderInResourceResponse.class, "FAPI-1-6.2.1-11");

		callAndStopOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, "FAPI-1-6.2.1-12");

		call(EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "FAPI-1-6.2.1-12");

		callAndStopOnFailure(EnsureResourceResponseContentTypeIsJsonUTF8.class, "FAPI-1-6.2.1-9", "FAPI-1-6.2.1-10");

		fireTestFinished();
		stop();

		return new ModelAndView("complete", ImmutableMap.of("test", this));

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttpMtls(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Unexpected HTTP call: " + path);
	}

}
