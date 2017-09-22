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
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.TestModule;
import io.fintechlabs.testframework.testmodule.TestModuleEventListener;

/**
 * @author jricher
 *
 */
public class SampleTestModule implements TestModule {

	public static final String name = "sample-test";
	private String id = null;
	private Status status = Status.UNKNOWN;
	private JsonObject config;
	private EventLog eventLog;
	private List<TestModuleEventListener> listeners = new ArrayList<>();
	private BrowserControl browser;
	private String testStateValue;
	private Result result = Result.UNKNOWN;
	private Map<String, String> exposed = new HashMap<>();
	private Environment env = new Environment();

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
		this.config = config;
		this.eventLog = eventLog;
		this.browser = browser;
		

		// TODO: this is an awkward way to call this
		env.put("base_url", baseUrl);
		new CreateRedirectUri().evaluate(env, id, eventLog);

		// this is inserted by the create call above, expose it to the test environment
		exposeEnvString("redirect_uri");
		
		this.status = Status.CONFIGURED;
		fireSetupDone();
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
		
		eventLog.log(getId(), "Redirecting to url" + redirectTo);

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

		eventLog.log(getId(), "Finsihed");
		
		this.status = Status.FINISHED;
		
		if (getResult().equals(Result.UNKNOWN)) {
			fireInterrupted();
		}
		
	}

	private void fireSetupDone() {
		for (TestModuleEventListener listener : listeners) {
			listener.setupDone();
		}
		
		eventLog.log(getId(), "Setup Done");
	}
	
	private void fireTestSuccess(){
		setResult(Result.PASSED);
		for (TestModuleEventListener listener : listeners) {
			listener.testSuccess();
		}
		eventLog.log(getId(), "Success");
	}
	
	private void fireTestFailure() {
		setResult(Result.FAILED);
		for (TestModuleEventListener listener : listeners) {
			listener.testFailure();
		}
		eventLog.log(getId(), "Failure");
	}
	
	private void fireInterrupted() {
		for (TestModuleEventListener listener : listeners) {
			listener.interrupted();
		}
		eventLog.log(getId(), "Interrupted");
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

		eventLog.log(getId(), "Path: " + path);
		eventLog.log(getId(), "Params: " + params);
		
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

				eventLog.log(getId(), "Token Endpoint error response:  " + e.getMessage());

				this.status = Status.FINISHED;
				fireTestFailure();
				return new ModelAndView("complete");
			}

			eventLog.log(getId(), "from TokenEndpoint jsonString = " + jsonString);

			JsonElement jsonRoot = new JsonParser().parse(jsonString);
			if (!jsonRoot.isJsonObject()) {
				eventLog.log(getId(), "Token Endpoint did not return a JSON object: " + jsonRoot);
				this.status = Status.FINISHED;
				fireTestFailure();
				return null;
			}

			JsonObject tokenResponse = jsonRoot.getAsJsonObject();

			if (tokenResponse.get("error") != null) {

				// Handle error

				String error = tokenResponse.get("error").getAsString();

				eventLog.log(getId(), "Token Endpoint returned: " + error);

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
					eventLog.log(getId(), "Token Endpoint did not return an access_token: " + jsonString);
					this.status = Status.FINISHED;
					fireTestFailure();

				}

				if (tokenResponse.has("id_token")) {
					idTokenValue = tokenResponse.get("id_token").getAsString();
				} else {
					eventLog.log(getId(), "Token Endpoint did not return an id_token");
				}

				if (tokenResponse.has("refresh_token")) {
					refreshTokenValue = tokenResponse.get("refresh_token").getAsString();
				}
				
				eventLog.log(getId(), refreshTokenValue);
				this.status = Status.FINISHED;
				fireTestSuccess();
				return new ModelAndView("/complete.html");
			}	
		} else {
			// no state value
			eventLog.log(getId(), "State value mismatch");
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
