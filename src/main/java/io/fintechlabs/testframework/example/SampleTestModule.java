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
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.CreateRedirectUri;
import io.fintechlabs.testframework.condition.GetServerConfiguration;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Condition;
import io.fintechlabs.testframework.testmodule.ConditionError;
import io.fintechlabs.testframework.testmodule.Environment;
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
	private String testStateValue;
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
		
		evaluate(CreateRedirectUri.class);

		// this is inserted by the create call above, expose it to the test environment for publication
		exposeEnvString("redirect_uri");

		// Make sure we're calling the right server configuration
		evaluate(GetServerConfiguration.class);

		this.status = Status.CONFIGURED;
		fireSetupDone();
	}

	/**
	 * @param class1
	 */
	private void evaluate(Class<? extends Condition> conditionClass) {
		try {
			
			// create a new condition object from the class above
			Condition condition = conditionClass
				.getDeclaredConstructor(String.class, EventLog.class)
				.newInstance(id, eventLog);

			// evaluate the condition and assign its results back to our environment
			env = condition.evaluate(env);
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			logger.error("Couldn't create required condition object", e);
		} catch (ConditionError error) {
			logger.info("Test condition failure", error);
			fireTestFailure();
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
		
		this.testStateValue = RandomStringUtils.randomAlphanumeric(10);
		
		// send a front channel request to start things off
		String redirectTo = UriComponentsBuilder.fromHttpUrl("https://mitreid.org/authorize")
				.queryParam("client_id", "client")
				.queryParam("response_type", "code")
				.queryParam("state", testStateValue)
				.queryParam("redirect_uri", env.getString("redirect_uri"))
				.build().toUriString();
		
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
		
	}

	private void fireSetupDone() {
		for (TestModuleEventListener listener : listeners) {
			listener.setupDone();
		}
		
		eventLog.log(getId(), getName(), "Setup Done");
	}
	
	private void fireTestSuccess(){
		setResult(Result.PASSED);
		for (TestModuleEventListener listener : listeners) {
			listener.testSuccess();
		}
		eventLog.log(getId(), getName(), "Success");
	}
	
	private void fireTestFailure() {
		setResult(Result.FAILED);
		for (TestModuleEventListener listener : listeners) {
			listener.testFailure();
		}
		eventLog.log(getId(), getName(), "Failure");
	}
	
	private void fireInterrupted() {
		for (TestModuleEventListener listener : listeners) {
			listener.interrupted();
		}
		eventLog.log(getId(), getName(), "Interrupted");
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
			// TODO: return an error page here for an unhandled page
			return new ModelAndView("/complete.html");
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
		
		if (params.containsKey("state") && params.getFirst("state").equals(testStateValue)) {

			String authorizationCode = params.getFirst("code");
			
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("grant_type", "authorization_code");
			form.add("code", authorizationCode);
			
			form.add("redirect_uri", env.getString("redirect_uri"));

			// Handle Token Endpoint interaction

			HttpClient httpClient = HttpClientBuilder.create()
					.useSystemProperties()
					.build();

			HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
			
			RestTemplate restTemplate = new RestTemplate(factory);

			//Alternatively use form based auth
			form.add("client_id", "client");
			form.add("client_secret", "secret");
			String jsonString = null;

			try {
				jsonString = restTemplate.postForObject("https://mitreid.org/token", form, String.class);
			} catch (RestClientException e) {

				// Handle error

				eventLog.log(getId(), getName(), "Token Endpoint error response:  " + e.getMessage());

				this.status = Status.FINISHED;
				fireTestFailure();
				return new ModelAndView("complete");
			}

			eventLog.log(getId(), getName(), "from TokenEndpoint jsonString = " + jsonString);

			JsonElement jsonRoot = new JsonParser().parse(jsonString);
			if (!jsonRoot.isJsonObject()) {
				eventLog.log(getId(), getName(), "Token Endpoint did not return a JSON object: " + jsonRoot);
				this.status = Status.FINISHED;
				fireTestFailure();
				return null;
			}

			JsonObject tokenResponse = jsonRoot.getAsJsonObject();

			if (tokenResponse.get("error") != null) {

				// Handle error

				String error = tokenResponse.get("error").getAsString();

				eventLog.log(getId(), getName(), "Token Endpoint returned: " + error);

				this.status = Status.FINISHED;
				fireTestFailure();
				return null;
			} else {

				// get out all the token strings
				String accessTokenValue = null;
				String idTokenValue = null;
				String refreshTokenValue = null;

				if (tokenResponse.has("access_token")) {
					accessTokenValue = tokenResponse.get("access_token").getAsString();
				} else {
					eventLog.log(getId(), getName(), "Token Endpoint did not return an access_token: " + jsonString);
					this.status = Status.FINISHED;
					fireTestFailure();

				}

				if (tokenResponse.has("id_token")) {
					idTokenValue = tokenResponse.get("id_token").getAsString();
				} else {
					eventLog.log(getId(), getName(), "Token Endpoint did not return an id_token");
				}

				if (tokenResponse.has("refresh_token")) {
					refreshTokenValue = tokenResponse.get("refresh_token").getAsString();
				}
				
				eventLog.log(getId(), getName(), refreshTokenValue);
				this.status = Status.FINISHED;
				fireTestSuccess();
				return new ModelAndView("/complete.html");
			}	
		} else {
			// no state value
			eventLog.log(getId(), getName(), "State value mismatch");
			fireTestFailure();
			this.status = Status.FINISHED;
			return null;
		}
			
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

}
