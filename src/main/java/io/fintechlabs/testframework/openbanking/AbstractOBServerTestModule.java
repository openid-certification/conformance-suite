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

package io.fintechlabs.testframework.openbanking;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CallAccountRequestsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken;
import io.fintechlabs.testframework.condition.client.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForDateHeaderInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForScopesInTokenResponse;
import io.fintechlabs.testframework.condition.client.CheckIfAccountRequestsEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
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
import io.fintechlabs.testframework.condition.client.EnsureResourceResponseEncodingIsUTF8;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractAccountRequestIdFromAccountRequestsEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromClientConfiguration;
import io.fintechlabs.testframework.condition.client.ExtractMTLSCertificatesFromConfiguration;
import io.fintechlabs.testframework.condition.client.FetchServerKeys;
import io.fintechlabs.testframework.condition.client.GenerateResourceEndpointRequestHeaders;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetResourceEndpointConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticServerConfiguration;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.ValidateStateHash;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipherForResourceEndpoint;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTls12;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;

public abstract class AbstractOBServerTestModule extends AbstractTestModule {

	public AbstractOBServerTestModule(String name, String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(name, id, owner, eventLog, browser, testInfo);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, java.lang.String)
	 */
	@Override
	public final void configure(JsonObject config, String baseUrl) {
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

		// Test won't pass without MATLS, but we'll try anyway (for now)
		call(ExtractMTLSCertificatesFromConfiguration.class, ConditionResult.WARNING);

		// Set up the resource endpoint configuration
		callAndStopOnFailure(GetResourceEndpointConfiguration.class);

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

		requestClientCredentialsGrant();

		createAccountRequest();

		createAuthorizationRequest();

		createAuthorizationRedirect();

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getName(), "Redirecting to url " + redirectTo);

		browser.goToUrl(redirectTo);

		setStatus(Status.WAITING);
	}

	protected void requestClientCredentialsGrant() {

		createClientCredentialsRequest();

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);
	}

	protected abstract void createClientCredentialsRequest();

	protected void createAccountRequest() {

		callAndStopOnFailure(CreateCreateAccountRequestRequest.class);

		callAndStopOnFailure(CallAccountRequestsEndpointWithBearerToken.class);

		callAndStopOnFailure(CheckIfAccountRequestsEndpointResponseError.class);

		call(CheckForFAPIInteractionIdInResourceResponse.class, "FAPI-1-6.2.1-12");

		callAndStopOnFailure(ExtractAccountRequestIdFromAccountRequestsEndpointResponse.class);
	}

	protected void createAuthorizationRequest() {

		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);
	}

	protected void createAuthorizationRedirect() {

		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	protected void onAuthorizationCallbackResponse() {

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		callAndStopOnFailure(CheckMatchingStateParameter.class);

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		performPostAuthorizationFlow();
	}

	protected void performPostAuthorizationFlow() {

		// call the token endpoint and complete the flow

		requestAuthorizationCode();

		requestProtectedResource();
	}

	protected abstract void createAuthorizationCodeRequest();

	protected void requestAuthorizationCode() {

		createAuthorizationCodeRequest();

		callAndStopOnFailure(CallTokenEndpoint.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-1-5.2.2-14");

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndStopOnFailure(CheckForScopesInTokenResponse.class, "FAPI-1-5.2.2-15");

		call(ValidateStateHash.class, ConditionResult.FAILURE, "FAPI-2-5.2.2-4");

		call(CheckForRefreshTokenValue.class);

		callAndStopOnFailure(EnsureMinimumTokenLength.class, "FAPI-1-5.2.2-16");

		call(EnsureMinimumTokenEntropy.class, ConditionResult.FAILURE, "FAPI-1-5.2.2-16");
	}

	protected void requestProtectedResource() {

		// verify the access token against a protected resource

		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);

		// FIXME: for now, run tests even if TLS1.0/1.1 or insecure ciphers are present on the server
		call(EnsureTls12.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowTLS10.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowTLS11.class, ConditionResult.FAILURE, "FAPI-2-8.5-2");
		call(DisallowInsecureCipherForResourceEndpoint.class, ConditionResult.FAILURE, "FAPI-2-8.5-1");

		callAndStopOnFailure(CallAccountsEndpointWithBearerToken.class, "FAPI-1-6.2.1-3");

		call(DisallowAccessTokenInQuery.class, ConditionResult.FAILURE, "FAPI-1-6.2.1-4");

		call(CheckForDateHeaderInResourceResponse.class, ConditionResult.FAILURE, "FAPI-1-6.2.1-11");

		call(CheckForFAPIInteractionIdInResourceResponse.class, ConditionResult.FAILURE, "FAPI-1-6.2.1-12");

		call(EnsureMatchingFAPIInteractionId.class, ConditionResult.FAILURE, "FAPI-1-6.2.1-12");

		call(EnsureResourceResponseEncodingIsUTF8.class, ConditionResult.FAILURE, "FAPI-1-6.2.1-9");
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		throw new TestFailureException(getId(), "Unexpected HTTP call: " + path);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttpMtls(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		throw new TestFailureException(getId(), "Unexpected HTTP call: " + path);
	}

	protected void logClientSecretWarning() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("msg", "client_secret_basic and client_secret_post are not recommended client authentication methods");
		map.put("result", ConditionResult.WARNING);
		map.put("requirements", Sets.newHashSet("OB-5.2.2-7.2"));
		eventLog.log(getName(), map);
	}

}
