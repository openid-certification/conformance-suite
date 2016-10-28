/*******************************************************************************
 * Copyright 2016 The MITRE Corporation
 *   and the MIT Internet Trust Consortium
 *
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

package io.bspk.testframework.strawman;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.ui.Model;

import com.google.gson.JsonObject;

/**
 * @author jricher
 *
 */
public class SampleTestModule implements TestModule {

	private String id;
	private Status status;
	private JsonObject config;
	private EventLog eventLog;
	private List<TestModuleEventListener> listeners;
	private BrowserControl browser;
	/**
	 * @param browser the browser to set
	 */
	public void setBrowser(BrowserControl browser) {
		this.browser = browser;
	}

	/**
	 * @param dispatcher the dispatcher to set
	 */
	public void setDispatcher(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	private Dispatcher dispatcher;
	
	/**
	 * 
	 */
	public SampleTestModule() {
		this.id = UUID.randomUUID().toString();
		
		this.listeners = new ArrayList<>();
		
		this.status = Status.CREATED;
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#configure(com.google.gson.JsonObject)
	 */
	public void configure(JsonObject config, EventLog eventLog) {
		this.config = config;
		this.eventLog = eventLog;
		this.status = Status.CONFIGURED;
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
		this.status = Status.RUNNING;
		
		String backChannelUrl = dispatcher.registerUrl("/backchannel**", backChannelHandler());
		
		String frontChannelUrl = dispatcher.registerUrl("/frontchannel**", frontChannelResponseHandler());
		
		// send a front channel request to start things off
		String redirctTo = "https://mitreid.org/authorize?client_id=client&response_type=code&redirect_uri=" + frontChannelUrl;
		
		eventLog.log("Redirecting to url" + redirctTo);
		browser.goToUrl(redirctTo);
	}
	
	/**
	 * @return
	 */
	private HttpHandlerMethod frontChannelResponseHandler() {
		return new HttpHandlerMethod() {
			@Override
			public String handle(Model m, 
					Map<String, String> parameters, 
					HttpServletRequest req, 
					HttpServletResponse res, 
					HttpSession session) {
				
				// TODO: how do we return a response without views? Or can we allow tests to inject their own views?
				return "display";
			}
		
		};
	}

	/**
	 * @return
	 */
	private HttpHandlerMethod backChannelHandler() {
		return new HttpHandlerMethod() {
			@Override
			public String handle(Model m, 
					Map<String, String> parameters, 
					HttpServletRequest req, 
					HttpServletResponse res, 
					HttpSession session) {
				
				// TODO: how do we return a response without views? Or can we allow tests to inject their own views?
				return "json";
			}

		};
	}

	/**
	 * @param e
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean addListener(TestModuleEventListener e) {
		return listeners.add(e);
	}

	/**
	 * @param o
	 * @return
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean removeListener(TestModuleEventListener o) {
		return listeners.remove(o);
	}

}
