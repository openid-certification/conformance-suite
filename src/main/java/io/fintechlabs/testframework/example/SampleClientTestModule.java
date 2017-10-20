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

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AuthenticateClientWithClientSecret;
import io.fintechlabs.testframework.condition.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.CreateAuthorizationCode;
import io.fintechlabs.testframework.condition.CreateTokenEndpointResponse;
import io.fintechlabs.testframework.condition.EnsureClientIsAuthenticated;
import io.fintechlabs.testframework.condition.EnsureMatchingClientId;
import io.fintechlabs.testframework.condition.EnsureMatchingRedirectUri;
import io.fintechlabs.testframework.condition.ExtractBearerAccessTokenFromHeader;
import io.fintechlabs.testframework.condition.ExtractBearerAccessTokenFromParams;
import io.fintechlabs.testframework.condition.ExtractClientCredentialsFromFormPost;
import io.fintechlabs.testframework.condition.ExtractRequestedScopes;
import io.fintechlabs.testframework.condition.FilterUserInfoForScopes;
import io.fintechlabs.testframework.condition.GenerateBearerAccessToken;
import io.fintechlabs.testframework.condition.GenerateIdTokenClaims;
import io.fintechlabs.testframework.condition.GenerateServerConfiguration;
import io.fintechlabs.testframework.condition.GetStaticClientConfiguration;
import io.fintechlabs.testframework.condition.GetStaticServerConfiguration;
import io.fintechlabs.testframework.condition.LoadJWKs;
import io.fintechlabs.testframework.condition.LoadUserInfo;
import io.fintechlabs.testframework.condition.RedirectBackToClientWithAuthorizationCode;
import io.fintechlabs.testframework.condition.RequireBearerAccessToken;
import io.fintechlabs.testframework.condition.RequireOpenIDScope;
import io.fintechlabs.testframework.condition.SignIdToken;
import io.fintechlabs.testframework.condition.ValidateAuthorizationCode;
import io.fintechlabs.testframework.condition.ValidateRedirectUri;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.AbstractTestModule;
import io.fintechlabs.testframework.testmodule.TestModule.Status;

/**
 * @author jricher
 *
 */
public class SampleClientTestModule extends AbstractTestModule {

	/**
	 * @param name
	 */
	public SampleClientTestModule() {
		super("sample-client-test");
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, io.fintechlabs.testframework.logging.EventLog, java.lang.String, io.fintechlabs.testframework.frontChannel.BrowserControl, java.lang.String)
	 */
	@Override
	public void configure(JsonObject config, String baseUrl) {
		env.putString("base_url", baseUrl);
		env.put("config", config);

		require(GenerateServerConfiguration.class);
		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");
		
		require(CheckServerConfiguration.class);
		
		require(LoadJWKs.class);
		
		require(LoadUserInfo.class);
		
		require(GetStaticClientConfiguration.class);

		
		
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
	 * @see io.fintechlabs.testframework.testmodule.TestModule#stop()
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		setStatus(Status.FINISHED);
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
			return new ModelAndView("testError");
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

		optional(ExtractBearerAccessTokenFromHeader.class);
		optional(ExtractBearerAccessTokenFromParams.class);

		require(RequireBearerAccessToken.class);
		
		require(RequireOpenIDScope.class);
		
		require(FilterUserInfoForScopes.class);
		
		JsonObject user = env.get("user_info_endpoint_response");
		
		// at this point we can assume the test is fully done
		fireTestSuccess();
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
		
		optional(ExtractClientCredentialsFromFormPost.class);
		
		optional(AuthenticateClientWithClientSecret.class);
		
		require(EnsureClientIsAuthenticated.class);
		
		require(ValidateAuthorizationCode.class);
		
		require(ValidateRedirectUri.class);
				
		require(GenerateBearerAccessToken.class);
		
		require(GenerateIdTokenClaims.class);
		
		require(SignIdToken.class);
		
		require(CreateTokenEndpointResponse.class);

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

		require(EnsureMatchingClientId.class);
		
		require(EnsureMatchingRedirectUri.class);
		
		require(ExtractRequestedScopes.class);
		
		require(CreateAuthorizationCode.class);
		
		require(RedirectBackToClientWithAuthorizationCode.class);
		
		exposeEnvString("authorization_endpoint_response_redirect");
		
		String redirectTo = env.getString("authorization_endpoint_response_redirect");
		
		setStatus(Status.WAITING);

		return new RedirectView(redirectTo, false, false, false);
	
	}

}
