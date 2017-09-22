/** *****************************************************************************
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
package io.fintechlabs.testframework.runner;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.example.SampleTestModule;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.TestModule;

/**
 * 
 * GET /runner/available: list of available tests
 * GET /runner/running: list of running tests
 * POST /runner: create test
 * GET /runner/id: get test status
 * POST /runner/id: start test
 * DELETE /runner/id: cancel test
 * GET /runner/browser/id: get front-channel external URLs
 * POST /runner/browser/id/visit: mark front-channel external URL as visited
 * 
 * @author jricher
 *
 */
@Controller
public class TestRunner {

	private static final String BASE_URL = "http://localhost:8080";
	private static final String TEST_PATH = "/test/";

	private static Logger logger = LoggerFactory.getLogger(TestRunner.class);
    
	// collection of all currently running tests
	private Map<String, TestBundle> runningTests = new HashMap<>();
	
	// collection of aliases assigned to tests
	private Map<String, String> aliases = new HashMap<>();
	
	
	@Autowired
	private EventLog eventLog;

    @RequestMapping(value = "/runner/available", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getAvailableTests(Model m) {
    	List<String> testModuleNames = getTestModuleNames();
    	
    	return new ResponseEntity<>(testModuleNames, HttpStatus.OK);
    }
    
    
    @RequestMapping(value = "/runner", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createTest(@RequestParam("test") String testName, 
    		@RequestParam("alias") String alias,
    		@RequestBody String body, Model m) {
    	
        TestModule test = createTestModule(testName);
        
        logger.info("Created: " + testName);

        logger.info("Status of " + testName + ": " + test.getStatus());

        JsonObject config = new JsonParser().parse(body).getAsJsonObject();

        String id = RandomStringUtils.randomAlphanumeric(10);

        BrowserControl browser = new CollectingBrowserControl();

        TestBundle bundle = new TestBundle();
        bundle.test = test;
        bundle.browser = browser;
        
        runningTests.put(id, bundle);

        String baseUrl;
        if (!Strings.isNullOrEmpty(alias)) {
        	try {
	        	// create an alias for the test
	        	if (!createTestAlias(alias, id)) {
	        		// there was a failure in creating the test alias, return an error
	        		return new ResponseEntity<>(HttpStatus.CONFLICT);
	        	}
				baseUrl = BASE_URL + TEST_PATH + "a/" + UriUtils.encodePathSegment(alias, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// this should never happen, why is Java dumb
				e.printStackTrace();
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

        } else {
            baseUrl = BASE_URL + TEST_PATH + id;
        }
        

        test.configure(config, eventLog, id, browser, baseUrl);

        logger.info("Status of " + testName + ": " + test.getId() + ": " + test.getStatus());

        Map<String, String> map = new HashMap<>();
        map.put("name", testName);
        map.put("id", test.getId());
        map.put("url", baseUrl);
        
        return new ResponseEntity<>(map, HttpStatus.CREATED);

    }

    /**
	 * @param alias
	 * @param id
	 * @return
	 */
	private boolean createTestAlias(String alias, String id) {
		// first see if the alias is already in use
		if (aliases.containsKey(alias)) {
			// find the test that has the alias
			String existingId = aliases.get(alias);
			TestBundle bundle = runningTests.get(existingId);

			if (bundle != null) {
				// TODO: make the override configurable to allow for conflict of re-used aliases
				
				bundle.test.stop(); // stop the currently-running test
			}
		}
		
		aliases.put(alias, id);
		return true;
	}


	@RequestMapping(value = "/runner/{id}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> startTest(@PathVariable("id") String testId) {
    	TestBundle bundle = runningTests.get(testId);
    	if (bundle != null) {
    		TestModule test = bundle.test;
            Map<String, Object> map = new HashMap<>();
            map.put("name", test.getName());
            map.put("id", test.getId());
            map.put("status", test.getStatus());
            map.put("result", test.getResult());
            map.put("exposed", test.getExposedValues());

            logger.info("Status of " + test.getName() + ": " + test.getId() + ": " + test.getStatus());

            // TODO: fire this off in a background task thread?
            test.start();

            logger.info("Status of " + test.getName() + ": " + test.getId() + ": " + test.getStatus());

            return new ResponseEntity<>(map, HttpStatus.OK);
    		
    	} else {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}


    }
    
    @RequestMapping(value = "/runner/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getTestStatus(@PathVariable("id") String testId, Model m) {
    	logger.info("Getting status of " + testId);
    	
    	TestBundle bundle = runningTests.get(testId);
    	if (bundle != null) {
    		TestModule test = bundle.test;
            Map<String, Object> map = new HashMap<>();
            map.put("name", test.getName());
            map.put("id", test.getId());
            map.put("status", test.getStatus());
            map.put("result", test.getResult());
            map.put("exposed", test.getExposedValues());
            
            return new ResponseEntity<>(map, HttpStatus.OK);
    		
    	} else {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    }
    
    @DeleteMapping(value = "/runner/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> cancelTest(@PathVariable("id") String testId) {
    	logger.info("Canceling " + testId);
    	
    	TestBundle bundle = runningTests.get(testId);
    	if (bundle != null) {
    		TestModule test = bundle.test;

    		// stop the test
    		test.stop();
    		
    		// return its status
            Map<String, Object> map = new HashMap<>();
            map.put("name", test.getName());
            map.put("id", test.getId());
            map.put("status", test.getStatus());
            map.put("result", test.getResult());
            map.put("exposed", test.getExposedValues());
          
            return new ResponseEntity<>(map, HttpStatus.OK);
    	} else {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    }
    
    @RequestMapping(value = "/runner/running", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> getAllRunningTestIds(Model m) {
    	Set<String> testIds = runningTests.keySet();

    	return new ResponseEntity<Set<String>>(testIds, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/runner/browser/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getBrowserStatus(@PathVariable("id") String testId, Model m) {
    	logger.info("Getting status of " + testId);
    	
    	TestBundle bundle = runningTests.get(testId);
    	if (bundle != null) {
    		BrowserControl browser = bundle.browser;
    		Map<String, Object> map = new HashMap<>();
            map.put("id", testId);
            if (browser instanceof CollectingBrowserControl) {
            	map.put("urls", ((CollectingBrowserControl) browser).getUrls());
            	map.put("visited", ((CollectingBrowserControl) browser).getVisited());
            }
            
            return new ResponseEntity<>(map, HttpStatus.OK);
    		
    	} else {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    }

    @RequestMapping(value = "/runner/browser/{id}/visit", method = RequestMethod.POST)
    public ResponseEntity<String> visitBrowserUrl(@PathVariable("id") String testId, @RequestParam("url") String url, Model m) {
    	TestBundle bundle = runningTests.get(testId);
    	if (bundle != null) {
    		BrowserControl browser = bundle.browser;
    		browser.urlVisited(url);
    		
    		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    		
    	} else {
    		
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
    public Object handle(
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

        String testId = pathParts.next(); // used to route to the right test

        if (testId.equals("a")) {
        	// it's an aliased test, look it up
        	String alias = pathParts.next();
        	if (!aliases.containsKey(alias)) {
        		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        	}
        	testId = aliases.get(alias);
        }

        String restOfPath = Joiner.on("/").join(pathParts);

        if (!runningTests.containsKey(testId)) {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
    	TestBundle bundle = runningTests.get(testId);
    	if (bundle != null) {
    		TestModule test = bundle.test;
        
    		return test.handleHttp(restOfPath, req, res, session, params, m);
    	} else {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    }

    private static class TestBundle {
    	public TestModule test;
    	public BrowserControl browser;
    }

}
