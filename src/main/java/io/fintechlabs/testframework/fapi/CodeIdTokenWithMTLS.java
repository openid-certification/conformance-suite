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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AddClientAssertionToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.CheckForIdTokenValue;
import io.fintechlabs.testframework.condition.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.CreateClientAuthenticationAssertionClaims;
import io.fintechlabs.testframework.condition.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.condition.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.CreateRedirectUri;
import io.fintechlabs.testframework.condition.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.EnsureMinimumTokenEntropy;
import io.fintechlabs.testframework.condition.EnsureMinimumTokenLength;
import io.fintechlabs.testframework.condition.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.ExtractImplicitHashToCallbackResponse;
import io.fintechlabs.testframework.condition.ExtractJWKsFromClientConfiguration;
import io.fintechlabs.testframework.condition.ExtractMTLSCertificatesFromClientConfiguration;
import io.fintechlabs.testframework.condition.FetchServerKeys;
import io.fintechlabs.testframework.condition.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.GetStaticServerConfiguration;
import io.fintechlabs.testframework.condition.ParseIdToken;
import io.fintechlabs.testframework.condition.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.condition.SignClientAuthenticationAssertion;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

/**
 * @author jricher
 *
 */
public class CodeIdTokenWithMTLS extends AbstractTestModule {

	
	private static final Logger logger = LoggerFactory.getLogger(CodeIdTokenWithMTLS.class);

	
	/**
	 * @param name
	 */
	public CodeIdTokenWithMTLS(String id, EventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super("code-id-token-with-mtls", id, eventLog, browser, testInfo);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, java.lang.String)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.put("config", config);
		
		require(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		optional(GetDynamicServerConfiguration.class);
		optional(GetStaticServerConfiguration.class);
		
		
		// make sure the server configuration passes some basic sanity checks
		require(CheckServerConfiguration.class);
		
		require(FetchServerKeys.class);
		
		// Set up the client configuration
		require(GetStaticClientConfiguration.class);
		
		exposeEnvString("client_id");
		
		//require(ExtractJWKsFromClientConfiguration.class);
		require(ExtractMTLSCertificatesFromClientConfiguration.class);

		setStatus(Status.CONFIGURED);

		fireSetupDone();
		
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		setStatus(Status.RUNNING);
		
		require(CreateAuthorizationEndpointRequestFromClientInformation.class);

		require(CreateRandomStateValue.class);
		exposeEnvString("state");
		require(AddStateToAuthorizationEndpointRequest.class);

		require(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		require(AddNonceToAuthorizationEndpointRequest.class);
		
		require(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);
		
		require(BuildPlainRedirectToAuthorizationEndpoint.class);
		
		String redirectTo = env.getString("redirect_to_authorization_endpoint");
		
		eventLog.log(getId(), getName(), "Redirecting to url " + redirectTo);

		browser.goToUrl(redirectTo);
		
		setStatus(Status.WAITING);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#stop()
	 */
	@Override
	public void stop() {
		eventLog.log(getId(), getName(), "Finished");
		
		setStatus(Status.FINISHED);
		
		if (getResult().equals(Result.UNKNOWN)) {
			fireInterrupted();
		}
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		eventLog.log(getId(), getName(), "Path: " + path);
		eventLog.log(getId(), getName(), "Params: " + requestParts);
		
		// dispatch based on the path
		
		// these are all user-facing and will require user-facing error pages, so we wrap them
		
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

		require(CreateRandomImplicitSubmitUrl.class);

		setStatus(Status.WAITING);

		return new ModelAndView("implicitCallback", 
				ImmutableMap.of("test", this, 
					"implicitSubmitUrl", env.getString("implicit_submit", "fullUrl")));
	}
	
	private ModelAndView handleImplicitSubmission(JsonObject requestParts) {

		// process the callback
		setStatus(Status.RUNNING);
		
		String hash = requestParts.get("body").getAsString();
		
		logger.info("Hash: " + hash);
		
		env.putString("implicit_hash", hash);
		
		require(ExtractImplicitHashToCallbackResponse.class);
	
		require(CheckIfAuthorizationEndpointError.class);
		
		require(CheckMatchingStateParameter.class);
		
		// check the ID token from the hybrid response
		
		
		// call the token endpoint and complete the flow
		
		require(ExtractAuthorizationCodeFromAuthorizationResponse.class);
		
		require(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);

		require(AddClientIdToTokenEndpointRequest.class);
		
		require(CallTokenEndpoint.class);

		require(CheckIfTokenEndpointResponseError.class);

		require(CheckForAccessTokenValue.class);
		
		optional(CheckForIdTokenValue.class);
		
		optional(ParseIdToken.class);
		
		optional(CheckForRefreshTokenValue.class);
		
		require(EnsureMinimumTokenLength.class);
		
		optional(EnsureMinimumTokenEntropy.class);
		
		setStatus(Status.FINISHED);
		fireTestSuccess();
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
