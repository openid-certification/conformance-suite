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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.CreateRedirectUri;
import io.fintechlabs.testframework.condition.ExpectRequestObjectUnverifiableErrorPage;
import io.fintechlabs.testframework.condition.ExtractJWKsFromClientConfiguration;
import io.fintechlabs.testframework.condition.FetchServerKeys;
import io.fintechlabs.testframework.condition.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.GetStaticServerConfiguration;
import io.fintechlabs.testframework.condition.SerializeRequestObjectWithNullAlgorithm;
import io.fintechlabs.testframework.condition.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;

public class EnsureRequestObjectSignatureAlgorithmIsNotNull extends AbstractTestModule {

	public EnsureRequestObjectSignatureAlgorithmIsNotNull(String id, EventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super("ensure-request-object-signature-algorithm-is-not-null", id, eventLog, browser, testInfo);
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

		require(ExtractJWKsFromClientConfiguration.class);

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

		require(ConvertAuthorizationEndpointRequestToRequestObject.class);

		require(SerializeRequestObjectWithNullAlgorithm.class);

		require(BuildRequestObjectRedirectToAuthorizationEndpoint.class);

		String redirectTo = env.getString("redirect_to_authorization_endpoint");

		eventLog.log(getId(), getName(), "Redirecting to url " + redirectTo);

		require(ExpectRequestObjectUnverifiableErrorPage.class);

		browser.goToUrl(redirectTo);

		/**
		 * We never expect the browser to come back from here, our test is done
		 */

		// someone needs to review this by hand
		setResult(Result.REVIEW);

		stop();
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

		// If we get any kind of callback to this, it's an error: the authorization server should not ever respond the authorization request

		eventLog.log(getId(), getName(), ImmutableMap.of(
			"msg", "Receved unexpected incoming request",
			"path", path,
			"method", req.getMethod(),
			"requirements", ImmutableSet.of("FAPI-2-7.3-1"),
			"requestParts", requestParts
		));
		
		throw new TestFailureException(getId(), "Got an HTTP response on a call we weren't expecting");
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttpMtls(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Unexpected HTTP call: " + path);
	}

}
