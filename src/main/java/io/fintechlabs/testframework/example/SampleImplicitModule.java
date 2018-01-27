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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.client.AddNonceToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddStateToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.client.CheckForScopesInTokenResponse;
import io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import io.fintechlabs.testframework.condition.client.CreateRandomNonceValue;
import io.fintechlabs.testframework.condition.client.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.client.CreateRedirectUri;
import io.fintechlabs.testframework.condition.client.EnsureMinimumTokenEntropy;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractImplicitHashToTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.client.SetAuthorizationEndpointRequestResponseTypeToToken;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.CreateRandomImplicitSubmitUrl;
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
	testName = "sample-implicit-test",
	displayName = "Sample Implicit Client Test",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.client_secret",
		"client.scope"
	}
)
public class SampleImplicitModule extends AbstractTestModule {

	public static Logger logger = LoggerFactory.getLogger(SampleImplicitModule.class); 
	
	/**
	 * 
	 */
	public SampleImplicitModule(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo) {
		super(id, owner, eventLog, browser, testInfo);
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#configure(com.google.gson.JsonObject)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.put("config", config);
		
		callAndStopOnFailure(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		callAndStopOnFailure(GetDynamicServerConfiguration.class);
		
		// make sure the server configuration passes some basic sanity checks
		callAndStopOnFailure(CheckServerConfiguration.class);
		
		// Set up the client configuration
		callAndStopOnFailure(GetStaticClientConfiguration.class);
		
		exposeEnvString("client_id");

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
		
		callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToToken.class);
		
		callAndStopOnFailure(BuildPlainRedirectToAuthorizationEndpoint.class);
		
		String redirectTo = env.getString("redirect_to_authorization_endpoint");
		
		eventLog.log(getName(), args("msg", "Redirecting to authorization endpoint", 
				"redirect_to", redirectTo,
				"http", "redirect"));

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
		} else if (path.equals(env.getString("implicit_submit", "path"))) {
			return handleImplicitSubmission(requestParts);
		} else {
			return new ModelAndView("testError");
		}
		
	}

	@UserFacing
	private ModelAndView handleCallback(JsonObject requestParts) {
		setStatus(Status.RUNNING);

		callAndStopOnFailure(CreateRandomImplicitSubmitUrl.class);

		setStatus(Status.WAITING);

		return new ModelAndView("implicitCallback", 
				ImmutableMap.of("test", this, 
					"implicitSubmitUrl", env.getString("implicit_submit", "fullUrl")));
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
		
		callAndStopOnFailure(ExtractImplicitHashToTokenEndpointResponse.class);
	
		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);
		
		callAndStopOnFailure(CheckMatchingStateParameter.class);

		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(CheckForAccessTokenValue.class, "FAPI-1-5.2.2-14");
		
		callAndStopOnFailure(CheckForScopesInTokenResponse.class, "FAPI-1-5.2.2-15");

		call(ExtractIdTokenFromTokenResponse.class, "FAPI-1-5.2.2-24");
		
		call(CheckForRefreshTokenValue.class);
		
		callAndStopOnFailure(EnsureMinimumTokenEntropy.class, "FAPI-1-5.2.2-16");
		
		fireTestFinished();
		stop();

		return new ModelAndView("complete", ImmutableMap.of("test", this));

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttpMtls(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, com.google.gson.JsonObject)
	 */
	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		throw new TestFailureException(getId(), "Got an HTTP response on a call we weren't expecting");
	}

}
