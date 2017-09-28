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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.CreateRedirectUri;
import io.fintechlabs.testframework.condition.CreateTokenEndpointRequestForAuthorizationCodeGrant;
import io.fintechlabs.testframework.condition.ExtractAuthorizationCodeFromAuthorizationResponse;
import io.fintechlabs.testframework.condition.GetClientConfiguration;
import io.fintechlabs.testframework.condition.GetServerConfiguration;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.TestModule;
import io.fintechlabs.testframework.testmodule.TestModuleEventListener;

/**
 * @author jricher
 *
 */
public class SampleTestModule implements TestModule {

	private static Logger logger = LoggerFactory.getLogger(SampleTestModule.class); 
	
	public static final String name = "sample-test";
	private String id = null; // unique identifier for the test, set from the outside
	private Status status = Status.UNKNOWN; // current status of the test
	private Result result = Result.UNKNOWN; // results of running the test
	private EventLog eventLog;
	private List<TestModuleEventListener> listeners = new ArrayList<>();
	private BrowserControl browser;
	private Map<String, String> exposed = new HashMap<>(); // exposes runtime values to outside modules
	private Environment env = new Environment(); // keeps track of values at runtime

	/**
	 * 
	 */
	public SampleTestModule() {
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
		require(GetServerConfiguration.class);
		
		// make sure the server configuration passes some basic sanity checks
		require(CheckServerConfiguration.class);
		
		// Set up the client configuration
		require(GetClientConfiguration.class);
		
		exposeEnvString("client_id");

		this.status = Status.CONFIGURED;
		fireSetupDone();
	}

	/**
	 * Create and evaluate a Condition in the current environment. Throw a @TestFailureException if the Condition fails.
	 */
	private void require(Class<? extends Condition> conditionClass) {
		try {
			
			// create a new condition object from the class above
			Condition condition = conditionClass
				.getDeclaredConstructor(String.class, EventLog.class)
				.newInstance(id, eventLog);

			// evaluate the condition and assign its results back to our environment
			env = condition.evaluate(env);
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logger.error("Couldn't create required condition object", e);
			fireTestFailure();
			throw new TestFailureException(getId(), "Couldn't create required condition: " + conditionClass.getSimpleName());
		} catch (ConditionError error) {
			logger.info("Test condition failure: " + error.getMessage());
			fireTestFailure();
			throw new TestFailureException(error);
		}
	}
	
	/**
	 * Create and evaluate a Condition in the current environment. Log but ignore if the Condition fails.
	 */
	private void optional(Class<? extends Condition> conditionClass) {
		try {
			
			// create a new condition object from the class above
			Condition condition = conditionClass
				.getDeclaredConstructor(String.class, EventLog.class)
				.newInstance(id, eventLog);

			// evaluate the condition and assign its results back to our environment
			env = condition.evaluate(env);
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logger.error("Couldn't create optional condition object", e);
		} catch (ConditionError error) {
			logger.info("Ignoring optional condition failure: " + error.getMessage());
		}
	}
	
	/**
	 * Create and evaluate a Condition in the current environment. Throw a @TestFailureException if the Condition succeeds. This is the inverse of require().
	 */
	private void expectFailure(Class<? extends Condition> conditionClass) {
		try {
			
			// create a new condition object from the class above
			Condition condition = conditionClass
				.getDeclaredConstructor(String.class, EventLog.class)
				.newInstance(id, eventLog);

			// evaluate the condition and assign its results back to our environment
			env = condition.evaluate(env);
			
			// if we got here, the condition succeeded but we're expecting a failure so throw an error
			fireTestFailure();
			throw new TestFailureException(getId(), "Condition failure expected, but got success: " + conditionClass.getSimpleName());
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logger.error("Couldn't create required condition object", e);
			fireTestFailure();
			throw new TestFailureException(getId(), "Couldn't create required condition: " + conditionClass.getSimpleName());
		} catch (ConditionError error) {
			logger.info("Test condition failure as expected: " + error.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#getId()
	 */
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#getStatus()
	 */
	public Status getStatus() {
		return status;		
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#start()
	 */
	public void start() {
		
		if (this.status != Status.CONFIGURED) {
			throw new RuntimeException("Invalid State: " + this.status);
		}
		
		this.status = Status.RUNNING;
		
		env.putString("state", RandomStringUtils.randomAlphanumeric(10));
		exposeEnvString("state");
		
		require(BuildPlainRedirectToAuthorizationEndpoint.class);
		
		String redirectTo = env.getString("redirect_to_authorization_endpoint");
		
		eventLog.log(getId(), getName(), "Redirecting to url" + redirectTo);

		browser.goToUrl(redirectTo);
		
		this.status = Status.WAITING;
	}

	/**
	 * @param e
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	@Override
	public boolean addListener(TestModuleEventListener e) {
		return listeners.add(e);
	}

	/**
	 * @param o
	 * @return
	 * @see java.util.List#remove(java.lang.Object)
	 */
	@Override
	public boolean removeListener(TestModuleEventListener o) {
		return listeners.remove(o);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.TestModule#stop()
	 */
	@Override
	public void stop() {

		eventLog.log(getId(), getName(), "Finsihed");
		
		this.status = Status.FINISHED;
		
		if (getResult().equals(Result.UNKNOWN)) {
			fireInterrupted();
		}

		logFinalEnv();
		
	}

	private void logFinalEnv() {
		Map<String, Object> finalEnv = new HashMap<>();
		for (String key : env.allObjectIds()) {
			finalEnv.put(key, env.get(key));
		}
		
		eventLog.log(getId(), "final_env", finalEnv);
	}

	private void fireSetupDone() {
		for (TestModuleEventListener listener : listeners) {
			listener.setupDone();
		}
		
		eventLog.log(getId(), getName(), "Setup Done");

		logFinalEnv();
	}
	
	private void fireTestSuccess(){
		setResult(Result.PASSED);
		for (TestModuleEventListener listener : listeners) {
			listener.testSuccess();
		}
		eventLog.log(getId(), getName(), "Success");

		logFinalEnv();
	}
	
	private void fireTestFailure() {
		setResult(Result.FAILED);
		for (TestModuleEventListener listener : listeners) {
			listener.testFailure();
		}
		eventLog.log(getId(), getName(), "Failure");

		logFinalEnv();
	}
	
	private void fireInterrupted() {
		for (TestModuleEventListener listener : listeners) {
			listener.interrupted();
		}
		eventLog.log(getId(), getName(), "Interrupted");

		logFinalEnv();
	}

	
	/**
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, org.springframework.util.MultiValueMap, org.springframework.ui.Model)
	 */
	@Override
	public ModelAndView handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, MultiValueMap<String, String> params, Model m) {

		eventLog.log(getId(), getName(), "Path: " + path);
		eventLog.log(getId(), getName(), "Params: " + params);
		
		// dispatch based on the path
		if (path.equals("callback")) {
			return handleCallback(path, req, res, session, params, m);
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
	private ModelAndView handleCallback(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, MultiValueMap<String, String> params, Model m) {

		// process the callback
		this.status = Status.RUNNING;
		
		env.put("callback_params", mapToJsonObject(params));
		require(CheckIfAuthorizationEndpointError.class);
		
		require(CheckMatchingStateParameter.class);
		//expectFailure(CheckMatchingStateParameter.class);

		require(ExtractAuthorizationCodeFromAuthorizationResponse.class);
		
		require(CreateTokenEndpointRequestForAuthorizationCodeGrant.class);
		
		require(AddFormBasedClientSecretAuthenticationParameters.class);
		
		require(CallTokenEndpoint.class);

		require(CheckIfTokenEndpointResponseError.class);

		require(CheckForAccessTokenValue.class);
		
		optional(CheckForIdTokenValue.class);
		
		optional(CheckForRefreshTokenValue.class);
		
		this.status = Status.FINISHED;
		fireTestSuccess();
		return new ModelAndView("complete", ImmutableMap.of("test", this));
			
	}

	/**
	 * utility function to convert an incoming multi-value map to a JSonObject for storage
	 * @param params
	 * @return
	 */
	private JsonObject mapToJsonObject(MultiValueMap<String, String> params) {
		JsonObject o = new JsonObject();
		for (String key : params.keySet()) {
			o.addProperty(key, params.getFirst(key));
		}
		return o;
	}

	/**
	 * @return the result
	 */
	public Result getResult() {
		return result;
	}

	/**
	 * @param result the result to set
	 */
	private void setResult(Result result) {
		this.result = result;
	}
	
	private void expose(String key, String val) {
		exposed.put(key, val);
	}
	
	private void exposeEnvString(String key) {
		String val = env.getString(key);
		expose(key, val);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#getExposedValues()
	 */
	@Override
	public Map<String, String> getExposedValues() {
		return exposed;
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.TestModule#getBrowser()
	 */
	@Override
	public BrowserControl getBrowser() {
		return this.browser;
	}

}
