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

package io.fintechlabs.testframework.example;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddFormBasedClientSecretAuthenticationParameters;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForDateHeaderInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForIdTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForScopesInTokenResponse;
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateRandomFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.client.DisallowAccessTokenInQuery;
import io.fintechlabs.testframework.condition.client.EnsureMatchingFAPIInteractionId;
import io.fintechlabs.testframework.condition.client.EnsureMinimumTokenEntropy;
import io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetResourceEndpointConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.ParseIdToken;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import io.fintechlabs.testframework.condition.client.SetTLSTestHostToResourceEndpoint;
import io.fintechlabs.testframework.condition.client.ValidateIdToken;
import io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature;
import io.fintechlabs.testframework.condition.client.ValidateStateHash;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureMinimumClientSecretEntropy;
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
	testName = "sample-test", 
	displayName = "Sample AS Test",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.client_secret",
		"client.scope",
		"tls.testHost",
		"tls.testPort",
		"mtls.cert",
		"mtls.key",
		"mtls.ca",
		
	}
)
public class SampleTestModule extends AbstractTestModule {

	public static Logger logger = LoggerFactory.getLogger(SampleTestModule.class);
	/**
	 * 
	 */
	public SampleTestModule(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#configure(com.google.gson.JsonObject)
	 */
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.put("config", config);
		
		callAndStopOnFailure(SetTLSTestHostFromConfig.class);
		callAndStopOnFailure(EnsureTLS12.class, "FAPI-1-7.1-1");
		call(DisallowTLS10.class, "FAPI-1-7.1-1");
		call(DisallowTLS11.class, "FAPI-1-7.1-1");
		call(DisallowInsecureCipher.class, "FAPI-2-8.5-1");
		
		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Get the server's configuration
		call(GetDynamicServerConfiguration.class);
		
		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);

		// fetch or load the server's keys as needed
		callAndStopOnFailure(FetchServerKeys.class);
		
		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);
		
		callAndStopOnFailure(EnsureMinimumClientSecretEntropy.class, "RFC6819-5.1.4.2-2", "RFC6749-10.10");
		
		//require(ExtractJWKsFromClientConfiguration.class);
		
		//require(GenerateJWKsFromClientSecret.class);
		
		exposeEnvString("client_id");
		
		// Set up the resource endpoint configuration
		//callAndStopOnFailure(GetResourceEndpointConfiguration.class);
		
		setStatus(Status.CONFIGURED);
		fireSetupDone();
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#start()
	 */
	public void start() {
		
		setStatus(Status.RUNNING);
		
		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);
		
		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
		
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);
		
		String redirectTo = env.getString("redirect_to_authorization_endpoint");
		
		eventLog.log(getName(), "Redirecting to url " + redirectTo);

		browser.goToUrl(redirectTo);
		
		setStatus(Status.WAITING);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, org.springframework.util.MultiValueMap, org.springframework.ui.Model)
	 */
	@Override
	public ModelAndView handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		logIncomingHttpRequest(path, requestParts);
		
		// dispatch based on the path
		if (path.equals("callback")) {
			return handleCallback(requestParts);
		} else {
			return new ModelAndView("testError");
		}
		
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
	@UserFacing
	private ModelAndView handleCallback(JsonObject requestParts) {

		// process the callback
		setStatus(Status.RUNNING);
		
		env.put("callback_params", requestParts.get("params").getAsJsonObject());
		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);
		
		callAndStopOnFailure(CheckMatchingStateParameter.class);

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);
		
		callAndStopOnFailure(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);
		
		callAndStopOnFailure(AddFormBasedClientSecretAuthenticationParameters.class);
		//require(CreateClientAuthenticationAssertionClaims.class);
		
		//require(SignClientAuthenticationAssertion.class);
		
		//require(AddClientAssertionToTokenEndpointRequest.class);
		
		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-1-5.2.2-14");
		
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
		
		callAndStopOnFailure(CheckForIdTokenValue.class);
		
		callAndStopOnFailure(CheckForScopesInTokenResponse.class, "FAPI-1-5.2.2-15");
		
		callAndStopOnFailure(ParseIdToken.class, "FAPI-1-5.2.2-24");
		
		callAndStopOnFailure(ValidateIdToken.class, "FAPI-1-5.2.2-24");
		
		callAndStopOnFailure(ValidateIdTokenSignature.class, "FAPI-1-5.2.2-24");
		
		call(ValidateStateHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");
		
		call(CheckForRefreshTokenValue.class);
		
		callAndStopOnFailure(EnsureMinimumTokenEntropy.class, "FAPI-1-5.2.2-16");
		
		// verify the access token against a protected resource

		/*
		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		
		callAndStopOnFailure(SetTLSTestHostToResourceEndpoint.class);
		
		call(DisallowInsecureCipher.class, "FAPI-2-8.5-1");
		
		callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "FAPI-1-6.2.1-3");
		
		call(DisallowAccessTokenInQuery.class, "FAPI-1-6.2.1-4");
		
		callAndStopOnFailure(CheckForDateHeaderInResourceResponse.class, "FAPI-1-6.2.1-11");
		
		call(CheckForFAPIInteractionIdInResourceResponse.class, "FAPI-1-6.2.1-12");
		
		call(EnsureMatchingFAPIInteractionId.class, "FAPI-1-6.2.1-12");
		
		call(EnsureResourceResponseEncodingIsUTF8.class, "FAPI-1-6.2.1-9");
		*/
		
		fireTestFinished();
		stop();

		return new ModelAndView("complete", ImmutableMap.of("test", this));
			
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttpMtls(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		/*
		eventLog.log(getId(), getName() + " MTLS Routing", requestParts.get("headers").getAsJsonObject());
		
		return new ModelAndView("complete", ImmutableMap.of("test", this));
		*/
		throw new TestFailureException(getId(), "Got an HTTP response on a call we weren't expecting");

	}

}
