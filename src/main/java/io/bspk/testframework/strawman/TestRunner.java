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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author jricher
 *
 */
@Controller
public class TestRunner {
	
	private static Logger logger = LoggerFactory.getLogger(TestRunner.class);
	private Map<String, TestModule> tests = new HashMap<>();
	
	@RequestMapping("/runner")
	public String runner() {
		
		
		TestModule test = new SampleTestModule();
		
		logger.info("Created: " + test.getId());
		
		logger.info("Status of " + test.getId() + ": " + test.getStatus());
		
		
		JsonObject config = new JsonParser().parse("{}").getAsJsonObject();
		
		EventLog eventLog = new SampleEventLog(test.getId());
		
		String id = UUID.randomUUID().toString();
		
		tests.put(id, test);
		
		test.configure(config, eventLog, id);
		
		logger.info("Status of " + test.getId() + ": " + test.getStatus());
		
		test.start();
		
		logger.info("Status of " + test.getId() + ": " + test.getStatus());
		
		return "no";
	
	}

	@RequestMapping("/test/{test-name}/{test-id}/{path:.*}")
	public ModelAndView handle (
			@PathVariable("test-name") String testName, 
			@PathVariable("test-id") String testId,
			@PathVariable("path") String path,
			HttpServletRequest req, HttpServletResponse res,
			HttpSession session,
			@RequestParam MultiValueMap<String, String> params,
			Model m) {
		
		TestModule test = tests.get(testId);
		
		return test.handleHttp(path, req, res, session, params, m);
	}
	

}
