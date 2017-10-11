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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.CheckServerConfiguration;
import io.fintechlabs.testframework.condition.GetStaticServerConfiguration;
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
		this.status = Status.CREATED;
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#configure(com.google.gson.JsonObject, io.fintechlabs.testframework.logging.EventLog, java.lang.String, io.fintechlabs.testframework.frontChannel.BrowserControl, java.lang.String)
	 */
	@Override
	public void configure(JsonObject config, EventLog eventLog, String id, BrowserControl browser, String baseUrl) {
		this.id = id;
		this.eventLog = eventLog;
		this.browser = browser;
		
		env.putString("base_url", baseUrl);
		env.put("config", config);

		require(GenerateServerConfiguration.class);
		
		require(CheckServerConfiguration.class);
		
		exposeEnvString("discoveryUrl");
		exposeEnvString("issuer");
		
		this.status = Status.CONFIGURED;
		fireSetupDone();
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#start()
	 */
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#stop()
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, org.springframework.util.MultiValueMap, org.springframework.ui.Model)
	 */
	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject params, Model m) {

		if (path.equals("authorize")) {
			return authorizationEndpoint(req, res, params, m);
		} else if (path.equals("token")) {
			return tokenEndpoint(req, res, params, m);
		} else if (path.equals("jwks")) {
			return jwksEndpoint(req, res, params, m);
		} else if (path.equals("register")) {
			return registrationEndpoint(req, res, params, m);
		} else if (path.equals("userinfo")) {
			return userinfoEndpoint(req, res, params, m);
		} else if (path.equals(".well-known/openid-configuration")) {
			return discoveryEndpoint(req, res, params, m);
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
	private Object discoveryEndpoint(HttpServletRequest req, HttpServletResponse res, JsonObject params, Model m) {
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
	private Object userinfoEndpoint(HttpServletRequest req, HttpServletResponse res, JsonObject params, Model m) {
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
	private Object registrationEndpoint(HttpServletRequest req, HttpServletResponse res, JsonObject params, Model m) {
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
	private Object jwksEndpoint(HttpServletRequest req, HttpServletResponse res, JsonObject params, Model m) {
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
	private Object tokenEndpoint(HttpServletRequest req, HttpServletResponse res, JsonObject params, Model m) {
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
	private Object authorizationEndpoint(HttpServletRequest req, HttpServletResponse res, JsonObject params, Model m) {
		// TODO
		return null;
	}

}
