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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.JsonObject;

/**
 * @author jricher
 *
 */
public class SampleTestModule implements TestModule {

	private static final String name = "sample-test";
	private String id = null;
	private Status status;
	private JsonObject config;
	private EventLog eventLog;
	private List<TestModuleEventListener> listeners;
	private BrowserControl browser;

	/**
	 * 
	 */
	public SampleTestModule() {
	
		this.listeners = new ArrayList<>();
		
		this.status = Status.CREATED;
	}

	/* (non-Javadoc)
	 * @see io.bspk.selenium.TestModule#configure(com.google.gson.JsonObject)
	 */
	public void configure(JsonObject config, EventLog eventLog, String id, BrowserControl browser) {
		this.id = id;
		this.config = config;
		this.eventLog = eventLog;
		this.browser = browser;
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
		
		if (this.status != Status.CONFIGURED) {
			throw new RuntimeException("WAT");
		}
		
		this.status = Status.RUNNING;
		
		// send a front channel request to start things off
		String redirctTo = "https://mitreid.org/authorize?client_id=client&response_type=code&redirect_uri=...";
		
		eventLog.log("Redirecting to url" + redirctTo);

		browser.goToUrl(redirctTo);
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
	 * @see io.bspk.testframework.strawman.TestModule#stop()
	 */
	@Override
	public void stop() {

		eventLog.log("Finsihed");
		
		this.status = Status.FINISHED;
		
	}

	/**
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see io.bspk.testframework.strawman.TestModule#handleHttp(java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession, org.springframework.util.MultiValueMap, org.springframework.ui.Model)
	 */
	@Override
	public ModelAndView handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, MultiValueMap<String, String> params, Model m) {

		eventLog.log("Path: " + path);
		eventLog.log("Params: " + params);
		
		return new ModelAndView("huh");
		
	}

}
