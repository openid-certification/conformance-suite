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
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
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

	@Value("${fintechlabs.base_url:http://localhost:8080}")
	private String baseUrl;

	private static Logger logger = LoggerFactory.getLogger(TestRunner.class);

	@Autowired
	private TestRunnerSupport support;
	
	@Autowired
	private EventLog eventLog;
	
	@Autowired
	private TestInfoService testInfo;

	@Autowired
	private AuthenticationFacade authenticationFacade;
	
	private Supplier<Map<String, TestModuleHolder>> testModuleSupplier = Suppliers.memoize(this::findTestModules);

	@RequestMapping(value = "/runner/available", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getAvailableTests(Model m) {
		
		Set<Map<String, String>> available = getTestModules().values().stream()
				.map(e -> ImmutableMap.of(
						"testName", e.a.testName(),
						"displayName", e.a.displayName(),
						"profile", e.a.profile()
						))
				.collect(Collectors.toSet());

		return new ResponseEntity<>(available, HttpStatus.OK);
	}
    
    
    @RequestMapping(value = "/runner", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createTest(@RequestParam("test") String testName,
    		@RequestBody JsonObject config, Model m) {

    	String id = RandomStringUtils.randomAlphanumeric(10);
    	
    	BrowserControl browser = new CollectingBrowserControl();
    	
        TestModule test = createTestModule(testName, id, browser);
        
        if (test == null) {
        	// return an error
        	return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        logger.info("Created: " + testName);

        logger.info("Status of " + testName + ": " + test.getStatus());

        support.addRunningTest(id, test);
        
        String url;
        String alias = "";

        // see if an alias was passed in as part of the configuration and use it if available
        if (config.has("alias") && config.get("alias").isJsonPrimitive()) {
	        	try {
	        		alias = config.get("alias").getAsString();
	        		
		        	// create an alias for the test
		        	if (!createTestAlias(alias, id)) {
		        		// there was a failure in creating the test alias, return an error
		        		return new ResponseEntity<>(HttpStatus.CONFLICT);
		        	}
				url = baseUrl + TestDispatcher.TEST_PATH + "a/" + UriUtils.encodePathSegment(alias, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// this should never happen, why is Java dumb
				e.printStackTrace();
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

        } else {
            url = baseUrl + TestDispatcher.TEST_PATH + id;
        }
        
        // log the test creation event in the event log
        Map<String, Object> testCreated = ImmutableMap.of(
        		"baseUrl", url,
        		"config", config,
        		"alias", alias,
        		"testName", testName);

        
        // add this test to the stack
        testInfo.createTest(id, testName, url, config, alias, Instant.now());

		eventLog.log(id, "TEST-RUNNER", test.getOwner(), testCreated);

        test.configure(config, url);

        logger.info("Status of " + testName + ": " + test.getId() + ": " + test.getStatus());

        Map<String, String> map = new HashMap<>();
        map.put("name", testName);
        map.put("id", test.getId());
        map.put("url", url);
        
        return new ResponseEntity<>(map, HttpStatus.CREATED);

    }

    /**
	 * @param alias
	 * @param id
	 * @return
	 */
	private boolean createTestAlias(String alias, String id) {
		// first see if the alias is already in use
		if (support.hasAlias(alias)) {
			// find the test that has the alias
			TestModule test = support.getRunningTestByAlias(alias);

			if (test != null) {
				// TODO: make the override configurable to allow for conflict of re-used aliases
				
				test.stop(); // stop the currently-running test
			}
		}
		
		support.addAlias(alias, id);
		return true;
	}


	@RequestMapping(value = "/runner/{id}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> startTest(@PathVariable("id") String testId) {
		TestModule test = support.getRunningTestById(testId);
    	if (test != null) {
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
    	
		TestModule test = support.getRunningTestById(testId);
    	if (test != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", test.getName());
            map.put("id", test.getId());
            map.put("status", test.getStatus());
            map.put("result", test.getResult());
            map.put("exposed", test.getExposedValues());
            map.put("owner", test.getOwner());
            
            return new ResponseEntity<>(map, HttpStatus.OK);
    		
    	} else {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    }
    
    @DeleteMapping(value = "/runner/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> cancelTest(@PathVariable("id") String testId) {
    	logger.info("Canceling " + testId);
    	
		TestModule test = support.getRunningTestById(testId);
    	if (test != null) {

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
    	Set<String> testIds = support.getAllRunningTestIds();

    	return new ResponseEntity<Set<String>>(testIds, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/runner/browser/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getBrowserStatus(@PathVariable("id") String testId, Model m) {
    	logger.info("Getting status of " + testId);
    	
		TestModule test = support.getRunningTestById(testId);
    	if (test != null) {
    		BrowserControl browser = test.getBrowser();
    		if (browser != null) {
	    		Map<String, Object> map = new HashMap<>();
	            map.put("id", testId);
	            if (browser instanceof CollectingBrowserControl) {
	            	map.put("urls", ((CollectingBrowserControl) browser).getUrls());
	            	map.put("visited", ((CollectingBrowserControl) browser).getVisited());
	            }
	            
	            return new ResponseEntity<>(map, HttpStatus.OK);
    		} else {
        		return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    		}
    	} else {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    }

    @RequestMapping(value = "/runner/browser/{id}/visit", method = RequestMethod.POST)
    public ResponseEntity<String> visitBrowserUrl(@PathVariable("id") String testId, @RequestParam("url") String url, Model m) {
		TestModule test = support.getRunningTestById(testId);
    	if (test != null) {
    		BrowserControl browser = test.getBrowser();
    		if (browser != null) {
	    		browser.urlVisited(url);
	    		
	    		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    		} else {
        		return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    		}
    		
    	} else {
    		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    	}
    }
    
    private TestModule createTestModule(String testName, String id, BrowserControl browser) {
    	
    	Class<? extends TestModule> testModuleClass = getTestModules().get(testName).c;
    	
    	TestModule module;
		try {
			
			@SuppressWarnings("unchecked")
			Map<String,String> owner = (ImmutableMap<String,String>)authenticationFacade.getAuthenticationToken().getPrincipal();
			
			TestInstanceEventLog wrappedEventLog = new TestInstanceEventLog(id, owner, eventLog);
			
			// call the constructor
			module = testModuleClass.getDeclaredConstructor(String.class, Map.class, TestInstanceEventLog.class, BrowserControl.class, TestInfoService.class)
				.newInstance(id, owner, wrappedEventLog, browser, testInfo);
			return module;
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			
			logger.warn("Couldn't create test module", e);

			return null;
		}
    	
    }
    
    private Map<String, TestModuleHolder> getTestModules() {
    		return testModuleSupplier.get();
    }
    
    private Map<String, TestModuleHolder> findTestModules() {
    	
    		Map<String, TestModuleHolder> testModules = new HashMap<>();
    	
    		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
    		scanner.addIncludeFilter(new AnnotationTypeFilter(PublishTestModule.class));
    		for (BeanDefinition bd : scanner.findCandidateComponents("io.fintechlabs")) {
    			try {
					Class<? extends TestModule> c = (Class<? extends TestModule>) Class.forName(bd.getBeanClassName());
					PublishTestModule a = c.getDeclaredAnnotation(PublishTestModule.class);
					
					testModules.put(a.testName(), new TestModuleHolder(c, a));
					
			} catch (ClassNotFoundException e) {
				logger.error("Couldn't load test module definition: " + bd.getBeanClassName());
			}
    		}
    		
    		return testModules;
    }
    
    private class TestModuleHolder {
    		public Class<? extends TestModule> c;
    		public PublishTestModule a;
			/**
			 * @param c
			 * @param a
			 */
			public TestModuleHolder(Class<? extends TestModule> c, PublishTestModule a) {
				this.c = c;
				this.a = a;
			}
    }
    
    // handle errors thrown by running tests
    @ExceptionHandler(TestFailureException.class)
    public ResponseEntity<Object> conditionFailure(TestFailureException error) {
    	try {
	    	TestModule test = support.getRunningTestById(error.getTestId());
	    	if (test != null) {
	    		logger.error("Caught an error while running the test, stopping the test: " + error.getMessage());
	    		test.stop();
	    	}
    	} catch (Exception e) {
    		logger.error("Something terrible happened when handling an error, I give up", e);
    	}

    	JsonObject obj = new JsonObject();
    	obj.addProperty("error", error.getMessage());
    	obj.addProperty("cause", error.getCause() != null ? error.getCause().getMessage() : null);
    	obj.addProperty("testId", error.getTestId());
    	return new ResponseEntity<>(obj, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
