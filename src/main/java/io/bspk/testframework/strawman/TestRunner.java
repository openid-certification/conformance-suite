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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
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
	
	private BrowserControl browser = new SampleBrowserController();
	
	@RequestMapping("/runner")
	public String runner() {
		
		
		TestModule test = new SampleTestModule();
		
		logger.info("Created: " + test.getName());
		
		logger.info("Status of " + test.getName() + ": " + test.getStatus());
		
		JsonObject config = new JsonParser().parse("{}").getAsJsonObject();
		
		
		//String id = UUID.randomUUID().toString();
		//String id = "HI";
		String id = RandomStringUtils.randomAlphanumeric(10);
		
		tests.put(id, test);
		EventLog eventLog = new SampleEventLog(id);
		
		String baseUrl = "http://localhost:8080" + "/test/" + test.getName() + "/" + id;
		
		test.configure(config, eventLog, id, browser, baseUrl);
		
		logger.info("Status of " + test.getName() + ": " + test.getId() + ": " + test.getStatus());
		
		test.start();
		
		logger.info("Status of " + test.getName() + ": " + test.getId() + ": " + test.getStatus());
		
		return "no";
	
	}

	@RequestMapping("/test/**")
	public ModelAndView handle (
//			@PathVariable("testname") String testName, 
//			@PathVariable("testid") String testId,
			//@PathVariable("path") String path,
			HttpServletRequest req, HttpServletResponse res,
			HttpSession session,
			@RequestParam MultiValueMap<String, String> params,
			Model m) {
		
		
		// TODO: explain why we're doing this by hand
		
		String path = (String) req.getAttribute(
	            HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
	    String bestMatchPattern = (String ) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

	    AntPathMatcher apm = new AntPathMatcher();
	    String finalPath = apm.extractPathWithinPattern(bestMatchPattern, path);

	    Iterator<String> pathParts = Splitter.on("/").split(finalPath).iterator();
	    
	    String testName = pathParts.next();
	    String testId = pathParts.next();
	    
	    String restOfPath = Joiner.on("/").join(pathParts);
	    
	    TestModule test = tests.get(testId);
		
		return test.handleHttp(restOfPath, req, res, session, params, m);
	}
	

}
