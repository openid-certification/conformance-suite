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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.BuildPlainRedirectToAuthorizationEndpointImplicit;
import io.fintechlabs.testframework.condition.CheckForAccessTokenValue;
import io.fintechlabs.testframework.condition.CheckForIdTokenValue;
import io.fintechlabs.testframework.condition.CheckForRefreshTokenValue;
import io.fintechlabs.testframework.condition.CheckIfAuthorizationEndpointError;
import io.fintechlabs.testframework.condition.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.CheckMatchingStateParameter;
import io.fintechlabs.testframework.condition.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.CreateRandomImplicitSubmitUrl;
import io.fintechlabs.testframework.condition.CreateRandomStateValue;
import io.fintechlabs.testframework.condition.CreateRedirectUri;
import io.fintechlabs.testframework.condition.EnsureMinimumTokenEntropy;
import io.fintechlabs.testframework.condition.ExtractImplicitHashToTokenEndpointResponse;
import io.fintechlabs.testframework.condition.GetDynamicServerConfiguration;
import io.fintechlabs.testframework.condition.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.ParseIdToken;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.UserFacing;

/**
 * @author jricher
 *
 */
public class SampleImplicitModule extends AbstractTestModule {

	public static Logger logger = LoggerFactory.getLogger(SampleImplicitModule.class); 
	
	/**
	 * 
	 */
	public SampleImplicitModule() {
		super("sample-implicit-test");
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
		
		require(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		require(GetDynamicServerConfiguration.class);
		
		// make sure the server configuration passes some basic sanity checks
		require(CheckServerConfiguration.class);
		
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
		
		require(BuildPlainRedirectToAuthorizationEndpointImplicit.class);
		
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
	public ModelAndView handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, MultiValueMap<String, String> params, Model m) {
		eventLog.log(getId(), getName(), "Path: " + path);
		eventLog.log(getId(), getName(), "Params: " + params);
		
		// dispatch based on the path
		
		// these are all user-facing and will require user-facing error pages, so we wrap them
		
		if (path.equals("callback")) {
			return handleCallback(path, req, res, session, params, m);
		} else if (path.equals(env.getString("implicit_submit", "path"))) {
			return handleImplicitSubmission(path, req, res, session, params, m);
		} else {
			return new ModelAndView("testError");
		}
		
	}

	@UserFacing
	private ModelAndView handleCallback(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, MultiValueMap<String, String> params, Model m) {
		require(CreateRandomImplicitSubmitUrl.class);
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
	private ModelAndView handleImplicitSubmission(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, MultiValueMap<String, String> params, Model m) {

		// process the callback
		this.status = Status.RUNNING;
		
		try {
			String hash = CharStreams.toString(req.getReader());
			
			logger.info("Hash: " + hash);
			
			env.putString("implicit_hash", hash);
			
			require(ExtractImplicitHashToTokenEndpointResponse.class);
		
			require(CheckIfAuthorizationEndpointError.class);
			
			require(CheckMatchingStateParameter.class);
	
			require(CheckIfTokenEndpointResponseError.class);
	
			require(CheckForAccessTokenValue.class);
			
			optional(CheckForIdTokenValue.class);
			
			optional(ParseIdToken.class);
			
			optional(CheckForRefreshTokenValue.class);
			
			require(EnsureMinimumTokenEntropy.class);
			
			this.status = Status.FINISHED;
			fireTestSuccess();
			return new ModelAndView("complete", ImmutableMap.of("test", this));
			
		} catch (IOException e) {
			throw new TestFailureException(getId(), e.getMessage());
		}
	}

}
