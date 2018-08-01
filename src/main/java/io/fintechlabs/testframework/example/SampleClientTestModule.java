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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.as.AddUserinfoUrlToServerConfiguration;
import io.fintechlabs.testframework.condition.as.AuthenticateClientWithClientSecret;
import io.fintechlabs.testframework.condition.as.CreateAuthorizationCode;
import io.fintechlabs.testframework.condition.as.CreateTokenEndpointResponse;
import io.fintechlabs.testframework.condition.as.EnsureClientIsAuthenticated;
import io.fintechlabs.testframework.condition.as.EnsureMatchingClientId;
import io.fintechlabs.testframework.condition.as.EnsureMatchingRedirectUri;
import io.fintechlabs.testframework.condition.as.EnsureMinimumKeyLength;
import io.fintechlabs.testframework.condition.as.ExtractClientCredentialsFromFormPost;
import io.fintechlabs.testframework.condition.as.ExtractRequestedScopes;
import io.fintechlabs.testframework.condition.as.FilterUserInfoForScopes;
import io.fintechlabs.testframework.condition.as.GenerateBearerAccessToken;
import io.fintechlabs.testframework.condition.as.GenerateIdTokenClaims;
import io.fintechlabs.testframework.condition.as.GenerateServerConfiguration;
import io.fintechlabs.testframework.condition.as.LoadJWKs;
import io.fintechlabs.testframework.condition.as.RedirectBackToClientWithAuthorizationCode;
import io.fintechlabs.testframework.condition.as.SignIdToken;
import io.fintechlabs.testframework.condition.as.ValidateAuthorizationCode;
import io.fintechlabs.testframework.condition.as.ValidateRedirectUri;
import io.fintechlabs.testframework.condition.client.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.common.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.common.EnsureMinimumClientSecretEntropy;
import io.fintechlabs.testframework.condition.rs.ExtractBearerAccessTokenFromHeader;
import io.fintechlabs.testframework.condition.rs.ExtractBearerAccessTokenFromParams;
import io.fintechlabs.testframework.condition.rs.LoadUserInfo;
import io.fintechlabs.testframework.condition.rs.RequireBearerAccessToken;
import io.fintechlabs.testframework.condition.rs.RequireOpenIDScope;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestExecutionManager;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;

/**
 * @author jricher
 *
 */
@PublishTestModule(
	testName = "sample-client-test",
	displayName = "Sample Client Test",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.client_secret",
		"client.scope",
		"client.redirect_uri"
	}
)
public class SampleClientTestModule extends AbstractTestModule {

	/**
	 * @param name
	 */
	public SampleClientTestModule(String id, Map<String, String> owner, TestInstanceEventLog eventLog, BrowserControl browser, TestInfoService testInfo, TestExecutionManager executionManager) {
		super(id, owner, eventLog, browser, testInfo, executionManager);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, io.fintechlabs.testframework.logging.EventLog, java.lang.String, io.fintechlabs.testframework.frontChannel.BrowserControl, java.lang.String)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.put("config", config);

		callAndStopOnFailure(GenerateServerConfiguration.class);
		callAndStopOnFailure(AddUserinfoUrlToServerConfiguration.class);
		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");

		callAndStopOnFailure(CheckServerConfiguration.class);

		callAndStopOnFailure(LoadJWKs.class);

		callAndStopOnFailure(EnsureMinimumKeyLength.class, "FAPI-1-5.2.2-5", "FAPI-1-5.2.2-6");

		callAndStopOnFailure(LoadUserInfo.class);

		callAndStopOnFailure(GetStaticClientConfiguration.class);

		call(EnsureMinimumClientSecretEntropy.class, ConditionResult.FAILURE, "RFC6819-5.1.4.2-2", "RFC6749-10.10");

		setStatus(Status.CONFIGURED);
		fireSetupDone();
		// this test can auto-start
		start();
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		// TODO Auto-generated method stub

		setStatus(Status.WAITING);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, org.springframework.util.MultiValueMap, org.springframework.ui.Model)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if (path.equals("authorize")) {
			return authorizationEndpoint(requestParts);
		} else if (path.equals("token")) {
			return tokenEndpoint(requestParts);
		} else if (path.equals("jwks")) {
			return jwksEndpoint();
		} else if (path.equals("register")) {
			return registrationEndpoint(requestParts);
		} else if (path.equals("userinfo")) {
			return userinfoEndpoint(requestParts);
		} else if (path.equals(".well-known/openid-configuration")) {
			return discoveryEndpoint();
		} else {
			throw new TestFailureException(getId(), "Got unexpected HTTP call to " + path);
		}

	}

	/**
	 * @param req
	 * @param res
	 * @param params
	 * @param m
	 * @return
	 */
	private Object discoveryEndpoint() {
		JsonObject serverConfiguration = env.get("server");

		return new ResponseEntity<Object>(serverConfiguration, HttpStatus.OK);
	}

	/**
	 * @param req
	 * @param res
	 * @param params
	 * @param m
	 * @return
	 */
	private Object userinfoEndpoint(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.put("incoming_request", requestParts);

		call(ExtractBearerAccessTokenFromHeader.class);
		call(ExtractBearerAccessTokenFromParams.class);

		callAndStopOnFailure(RequireBearerAccessToken.class);

		callAndStopOnFailure(RequireOpenIDScope.class, "FAPI-1-5.2.3-7");

		callAndStopOnFailure(FilterUserInfoForScopes.class);

		JsonObject user = env.get("user_info_endpoint_response");

		// at this point we can assume the test is fully done
		fireTestFinished();
		stop();

		return new ResponseEntity<Object>(user, HttpStatus.OK);

	}

	/**
	 * @param req
	 * @param res
	 * @param params
	 * @param m
	 * @return
	 */
	private Object registrationEndpoint(JsonObject requestParts) {

		//env.put("client_registration_request", requestParts.get("body_json"));

		// TODO Auto-generated method stub
		return null;

	}

	/**
	 * @param req
	 * @param res
	 * @param params
	 * @param m
	 * @return
	 */
	private Object jwksEndpoint() {

		setStatus(Status.RUNNING);
		JsonObject jwks = env.get("public_jwks");

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(jwks, HttpStatus.OK);
	}

	/**
	 * @param req
	 * @param res
	 * @param params
	 * @param m
	 * @return
	 */
	private Object tokenEndpoint(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.put("token_endpoint_request", requestParts);

		call(ExtractClientCredentialsFromFormPost.class);

		call(AuthenticateClientWithClientSecret.class);

		callAndStopOnFailure(EnsureClientIsAuthenticated.class);

		callAndStopOnFailure(ValidateAuthorizationCode.class);

		callAndStopOnFailure(ValidateRedirectUri.class);

		callAndStopOnFailure(GenerateBearerAccessToken.class);

		callAndStopOnFailure(GenerateIdTokenClaims.class);

		callAndStopOnFailure(SignIdToken.class);

		callAndStopOnFailure(CreateTokenEndpointResponse.class);

		setStatus(Status.WAITING);

		return new ResponseEntity<Object>(env.get("token_endpoint_response"), HttpStatus.OK);

	}

	/**
	 * @param req
	 * @param res
	 * @param params
	 * @param m
	 * @return
	 */
	private Object authorizationEndpoint(JsonObject requestParts) {

		setStatus(Status.RUNNING);

		env.put("authorization_endpoint_request", requestParts.get("params").getAsJsonObject());

		callAndStopOnFailure(EnsureMatchingClientId.class);

		callAndStopOnFailure(EnsureMatchingRedirectUri.class);

		callAndStopOnFailure(ExtractRequestedScopes.class);

		callAndStopOnFailure(CreateAuthorizationCode.class);

		callAndStopOnFailure(RedirectBackToClientWithAuthorizationCode.class);

		exposeEnvString("authorization_endpoint_response_redirect");

		String redirectTo = env.getString("authorization_endpoint_response_redirect");

		setStatus(Status.WAITING);

		return new RedirectView(redirectTo, false, false, false);

	}

}
