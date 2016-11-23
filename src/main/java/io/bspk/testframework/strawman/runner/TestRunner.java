/** *****************************************************************************
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
 ****************************************************************************** */
package io.bspk.testframework.strawman.runner;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.bspk.testframework.strawman.example.SampleBrowserController;
import io.bspk.testframework.strawman.example.SampleEventLog;
import io.bspk.testframework.strawman.example.SampleTestModule;
import io.bspk.testframework.strawman.frontChannel.BrowserControl;
import io.bspk.testframework.strawman.logging.EventLog;
import io.bspk.testframework.strawman.testmodule.TestModule;
import io.bspk.testframework.strawman.view.HttpCodeView;
import io.bspk.testframework.strawman.view.JsonEntityView;
import io.bspk.testframework.strawman.view.JsonErrorView;

/**
 * @author jricher
 *
 */
@Controller
public class TestRunner {

	private static final String BASE_URL = "http://localhost:8080";
	private static final String TEST_PATH = "/test/";

	private static Logger logger = LoggerFactory.getLogger(TestRunner.class);
    
	private Map<String, TestBundle> runningTests = new HashMap<>();

    @RequestMapping("/runner")
    public String runner() {
    	return null;
    }
    
    
    @RequestMapping(value = "/runner/available", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getAvailableTests(Model m) {
    	List<String> testModuleNames = getTestModuleNames();
    	
    	m.addAttribute(JsonEntityView.ENTITY, testModuleNames);
    	
    	return JsonEntityView.VIEWNAME;
    }
    
    
    @RequestMapping(value = "/runner/start", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String startTest(@RequestParam("test") String testName, @RequestBody String body, Model m) {
    	
        TestModule test = createTestModule(testName);

        logger.info("Created: " + test.getName());

        logger.info("Status of " + test.getName() + ": " + test.getStatus());

        JsonObject config = new JsonParser().parse(body).getAsJsonObject();

        String id = RandomStringUtils.randomAlphanumeric(10);

        EventLog eventLog = new SampleEventLog(id);

        String baseUrl = BASE_URL + TEST_PATH + test.getName() + "/" + id;
        
        BrowserControl browser = new CollectingBrowserControl();

        TestBundle bundle = new TestBundle();
        bundle.test = test;
        bundle.browser = browser;
        bundle.log = eventLog;
        
        runningTests.put(id, bundle);

        test.configure(config, eventLog, id, browser, baseUrl);

        logger.info("Status of " + test.getName() + ": " + test.getId() + ": " + test.getStatus());

        // TODO: fire this off in a background task thread?
        test.start();

        logger.info("Status of " + test.getName() + ": " + test.getId() + ": " + test.getStatus());

        Map<String, String> map = new HashMap<>();
        map.put("name", test.getName());
        map.put("id", test.getId());
        map.put("url", baseUrl);
        
        m.addAttribute(JsonEntityView.ENTITY, map);
        m.addAttribute(HttpCodeView.CODE, HttpStatus.CREATED);
        return JsonEntityView.VIEWNAME;

    }
    
    @RequestMapping(value = "/status/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getTestStatus(@PathVariable("id") String testId, Model m) {
    	logger.info("Getting status of " + testId);
    	
    	TestBundle bundle = runningTests.get(testId);
    	if (bundle != null) {
    		TestModule test = bundle.test;
            Map<String, String> map = new HashMap<>();
            map.put("name", test.getName());
            map.put("id", test.getId());
            map.put("status", test.getStatus().toString());
            
            m.addAttribute(JsonEntityView.ENTITY, map);
            m.addAttribute(HttpCodeView.CODE, HttpStatus.OK);
            return JsonEntityView.VIEWNAME;
    		
    	} else {
    		
    		m.addAttribute(HttpCodeView.CODE, HttpStatus.NOT_FOUND);
    		return JsonErrorView.VIEWNAME;
    	}
    }
    
    @RequestMapping(value = "/browser/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getBrowserStatus(@PathVariable("id") String testId, Model m) {
    	logger.info("Getting status of " + testId);
    	
    	TestBundle bundle = runningTests.get(testId);
    	if (bundle != null) {
    		BrowserControl browser = bundle.browser;
            Map<String, Object> map = new HashMap<>();
            map.put("id", testId);
            if (browser instanceof CollectingBrowserControl) {
            	map.put("urls", ((CollectingBrowserControl) browser).getUrls());
            }
            
            m.addAttribute(JsonEntityView.ENTITY, map);
            m.addAttribute(HttpCodeView.CODE, HttpStatus.OK);
            return JsonEntityView.VIEWNAME;
    		
    	} else {
    		
    		m.addAttribute(HttpCodeView.CODE, HttpStatus.NOT_FOUND);
    		return JsonErrorView.VIEWNAME;
    	}
    }

    // TODO: make this a factory bean
    private TestModule createTestModule(String testName) {
    	switch (testName) {
		case SampleTestModule.name:
			return new SampleTestModule();
		default:
			return null;
    	}
    }
    
    // TODO: make this a factory bean
    private List<String> getTestModuleNames() {
    	return ImmutableList.of(SampleTestModule.name);
    }
    
    @RequestMapping(TEST_PATH + "**")
    public ModelAndView handle(
            HttpServletRequest req, HttpServletResponse res,
            HttpSession session,
            @RequestParam MultiValueMap<String, String> params,
            Model m) {

        /*
         * We have to parse the path by hand so that we can match the substrings that apply
         * to the test itself and also pull out the query parameters to be passed on to
         * the underlying handler functions.
         */

    	String path = (String) req.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();
        String finalPath = apm.extractPathWithinPattern(bestMatchPattern, path);

        Iterator<String> pathParts = Splitter.on("/").split(finalPath).iterator();

        String testName = pathParts.next();
        String testId = pathParts.next();

        String restOfPath = Joiner.on("/").join(pathParts);

    	TestBundle bundle = runningTests.get(testId);
    	if (bundle != null) {
    		TestModule test = bundle.test;
        
    		//TODO: ensure test name matches for sanity check

    		return test.handleHttp(restOfPath, req, res, session, params, m);
    	} else {
    		m.addAttribute(HttpCodeView.CODE, HttpStatus.NOT_FOUND);
    		return new ModelAndView(HttpCodeView.VIEWNAME);
    	}
    }

    private static class TestBundle {
    	public TestModule test;
    	public BrowserControl browser;
    	public EventLog log;
    }

}
