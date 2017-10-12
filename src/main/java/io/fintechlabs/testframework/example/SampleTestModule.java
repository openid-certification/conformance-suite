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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AddFormBasedClientSecretAuthenticationParameters;
import io.fintechlabs.testframework.condition.BuildPlainRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.CallTokenEndpoint;
import io.fintechlabs.testframework.condition.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.CheckForIdTokenValue;
import io.fintechlabs.testframework.condition.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.CreateRedirectUri;
import io.fintechlabs.testframework.condition.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.EnsureMinimumTokenEntropy;
import io.fintechlabs.testframework.condition.EnsureTls12;
import io.fintechlabs.testframework.condition.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.FetchServerKeys;
import io.fintechlabs.testframework.condition.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.GetStaticServerConfiguration;
import io.fintechlabs.testframework.condition.ParseIdToken;
import io.fintechlabs.testframework.condition.ValidateIdToken;
import io.fintechlabs.testframework.condition.ValidateIdTokenSignature;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.UserFacing;

/**
 * @author jricher
 *
 */
public class SampleTestModule extends AbstractTestModule {

	public static Logger logger = LoggerFactory.getLogger(SampleTestModule.class); 
	
	/**
	 * 
	 */
	public SampleTestModule() {
		super("sample-test");
		this.status = Status.CREATED;
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#configure(com.google.gson.JsonObject)
	 */
	public void configure(JsonObject config, EventLog eventLog, String id, BrowserControl browser, String baseUrl) {
		this.id = id;
		this.eventLog = eventLog;
		this.browser = browser;
		
		env.putString("base_url", baseUrl);
		env.put("config", config);
		
		optional(EnsureTls12.class);
		
		require(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Get the server's configuration
		optional(GetDynamicServerConfiguration.class);
		
		// make sure the server configuration passes some basic sanity checks
		require(CheckServerConfiguration.class);

		// fetch or load the server's keys as needed
		require(FetchServerKeys.class);
		
		// Set up the client configuration
		require(GetStaticClientConfiguration.class);
		
		exposeEnvString("client_id");

		this.status = Status.CONFIGURED;
		fireSetupDone();
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#start()
	 */
	public void start() {
		
		if (this.status != Status.CONFIGURED) {
			throw new RuntimeException("Invalid State: " + this.status);
		}
		
		this.status = Status.RUNNING;
		
		require(CreateRandomStateValue.class);
		exposeEnvString("state");
		
		require(BuildPlainRedirectToAuthorizationEndpoint.class);
		
		String redirectTo = env.getString("redirect_to_authorization_endpoint");
		
		eventLog.log(getId(), getName(), "Redirecting to url" + redirectTo);

		browser.goToUrl(redirectTo);
		
		this.status = Status.WAITING;
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.TestModule#stop()
	 */
	@Override
	public void stop() {

		eventLog.log(getId(), getName(), "Finished");
		
		this.status = Status.FINISHED;
		
		if (getResult().equals(Result.UNKNOWN)) {
			fireInterrupted();
		}

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, org.springframework.util.MultiValueMap, org.springframework.ui.Model)
	 */
	@Override
	public ModelAndView handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		eventLog.log(getId(), getName(), "Path: " + path);
		eventLog.log(getId(), getName(), "Params: " + requestParts);
		
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
		this.status = Status.RUNNING;
		
		env.put("callback_params", requestParts.get("params").getAsJsonObject());
		require(CheckIfAuthorizationEndpointError.class);
		
		require(CheckMatchingStateParameter.class);

		require(ExtractAuthorizationCodeFromAuthorizationResponse.class);
		
		require(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);
		
		require(AddFormBasedClientSecretAuthenticationParameters.class);
		
		require(CallTokenEndpoint.class);

		require(CheckIfTokenEndpointResponseError.class);

		require(CheckForAccessTokenValue.class);
		
		require(CheckForIdTokenValue.class);
		
		require(ParseIdToken.class);
		
		require(ValidateIdToken.class);
		
		require(ValidateIdTokenSignature.class);
		
		optional(CheckForRefreshTokenValue.class);
		
		require(EnsureMinimumTokenEntropy.class);
		
		this.status = Status.FINISHED;
		fireTestSuccess();
		return new ModelAndView("complete", ImmutableMap.of("test", this));
			
	}

}
