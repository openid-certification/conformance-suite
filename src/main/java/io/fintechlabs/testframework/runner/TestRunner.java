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
import java.util.*;
import java.util.concurrent.*;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.frontChannel.BrowserControl;
import io.fintechlabs.testframework.info.TestInfoService;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.TestFailureException;
import io.fintechlabs.testframework.testmodule.TestModule;
import io.fintechlabs.testframework.testmodule.TestModule.Result;

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

	private ExecutorService executorService = Executors.newCachedThreadPool();
	private ExecutorCompletionService executorCompletionService = new ExecutorCompletionService(executorService);
	private Map<String, List<Future>> taskFutures = new HashMap<>();
	private FutureWatcher futureWatcher;

	// TODO: Move this stuff to it's own file?
	private class BackgroundTask implements Callable {
		private String testId;
		private Callable myCallable;

		public BackgroundTask(String testId, Callable callable) {
			this.testId = testId;
			this.myCallable = callable;
		}

		@Override
		public Object call() throws TestFailureException {
			Object returnObj = null;
			try {
				returnObj = myCallable.call();
			} catch (TestFailureException e) {
				throw e;
			} catch (Exception e) {
				throw new TestFailureException(testId, e.getMessage());
			}
			return returnObj;
		}
	}

	private class FutureWatcher implements Runnable {
		private boolean running = false;

		public void stop() {
			this.running = false;
		}

		@Override
		public void run() {
			running = true;
			while (running) {
				try {
					FutureTask future = (FutureTask) executorCompletionService.poll(1, TimeUnit.SECONDS);
					if (future != null && !future.isCancelled()) {
						future.get();
					}
				} catch (InterruptedException e) {
					// If we've been interrupted, then either it was on purpose, or something went very very wrong.
					logger.error("Background task was interrupted", e);
				} catch (ExecutionException e) {
					if (e.getCause().getClass().equals(TestFailureException.class)) {
						// This should always be the case for our BackgroundTasks
						TestFailureException testFailureException = (TestFailureException) e.getCause();

						// Clean up other tasks for this test id
						String testId = testFailureException.getTestId();
						for (Future f : taskFutures.get(testId)) {
							if (!f.isDone()) {
								f.cancel(true); // True allows the task to be interrupted.
							}
						}

						// We can't just throw it, the Exception Handler Annotation is only for HTTP requests
						conditionFailure(testFailureException);

						TestModule test = support.getRunningTestById(testId);
						if (test != null) {
							// there's an exception, stop the test
							test.stop();
							test.setFinalError(testFailureException);
							test.fireTestFailure();
						}

					} else {
						// TODO: Better handling if we get something we wern't expecting?
						logger.error("Execution failure", e);
						//eventLog.log(testId, "TEST RUNNER", authenticationFacade.getPrincipal(), EventLog.ex(e));
					}

				}
			}
		}
	}

	private void runInBackground(String testId, Callable callable) {
		if (futureWatcher == null) {
			futureWatcher = new FutureWatcher();
			executorService.submit(futureWatcher);
		}
		List<Future> futures;
		if (taskFutures.containsKey(testId)) {
			futures = taskFutures.remove(testId);
		} else {
			futures = new ArrayList<Future>();
		}
		futures.add(executorCompletionService.submit(new BackgroundTask(testId, callable)));
		taskFutures.put(testId, futures);
	}

	@RequestMapping(value = "/runner/available", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getAvailableTests(Model m) {

		Set<Map<String, ?>> available = getTestModules().values().stream()
			.map(e -> ImmutableMap.of(
				"testName", e.a.testName(),
				"displayName", e.a.displayName(),
				"profile", e.a.profile(),
				"configurationFields", e.a.configurationFields()))
			.collect(Collectors.toSet());

		return new ResponseEntity<>(available, HttpStatus.OK);
	}

	@RequestMapping(value = "/runner", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> createTest(@RequestParam("test") String testName, @RequestParam(name = "plan", required = false) String planId, @RequestBody JsonObject config, Model m) {

		String id = RandomStringUtils.randomAlphanumeric(10);

		TestModule test = createTestModule(testName, id, config);

		if (test == null) {
			// return an error
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		logger.info("Created: " + testName);

		// logger.info("Status of " + testName + ": " + test.getStatus());

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

		// record that this test was started
		testInfo.createTest(id, testName, url, config, alias, Instant.now(), planId);

		// log the test creation event in the event log
		eventLog.log(id, "TEST-RUNNER", test.getOwner(),
			EventLog.args("msg", "Test instance " + id + " created",
				"result", ConditionResult.INFO,
				"baseUrl", url,
				"config", config,
				"alias", alias,
				"planId", planId,
				"testName", testName));

		runInBackground(id, () -> {
			test.configure(config, url);
			return "done";
		});
		// logger.info("Status of " + testName + ": " + test.getId() + ": " + test.getStatus());

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

				eventLog.log(test.getId(), "TEST-RUNNER", test.getOwner(), EventLog.args("msg", "Stopping test due to alias conflict", "alias", alias, "new_test_id", id));

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
			Map<String, Object> map = createTestStatusMap(test);

			//logger.info("Status of " + test.getName() + ": " + test.getId() + ": " + test.getStatus());

			runInBackground(test.getId(), () -> {
				test.start();
				return "started";
			});

			//logger.info("Status of " + test.getName() + ": " + test.getId() + ": " + test.getStatus());

			return ResponseEntity.ok().body(map);

		} else {
			return ResponseEntity.notFound().build();
		}

	}

	@GetMapping(value = "/runner/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getTestStatus(@PathVariable("id") String testId, Model m) {
		//logger.info("Getting status of " + testId);

		TestModule test = support.getRunningTestById(testId);
		if (test != null) {
			Map<String, Object> map = createTestStatusMap(test);

			return ResponseEntity.ok().body(map);

		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@DeleteMapping(value = "/runner/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> cancelTest(@PathVariable("id") String testId) {
		// logger.info("Canceling " + testId);

		TestModule test = support.getRunningTestById(testId);
		if (test != null) {

			// stop the test
			runInBackground(test.getId(), () -> {
				eventLog.log(test.getId(), "TEST-RUNNER", test.getOwner(), EventLog.args("msg", "Stopping test from external request"));
				test.stop();
				return "stopped";
			});

			// return its immediate status
			Map<String, Object> map = createTestStatusMap(test);

			return new ResponseEntity<>(map, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(value = "/runner/running", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Set<String>> getAllRunningTestIds(Model m) {
		Set<String> testIds = support.getAllRunningTestIds();

		return new ResponseEntity<>(testIds, HttpStatus.OK);
	}

	@RequestMapping(value = "/runner/browser/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getBrowserStatus(@PathVariable("id") String testId, Model m) {
		// logger.info("Getting status of " + testId);

		TestModule test = support.getRunningTestById(testId);
		if (test != null) {
			BrowserControl browser = test.getBrowser();
			if (browser != null) {
				Map<String, Object> map = new HashMap<>();
				map.put("id", testId);
				map.put("urls", browser.getUrls());
				map.put("visited", browser.getVisited());
				map.put("runners", browser.getWebRunners());

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

	private TestModule createTestModule(String testName, String id, JsonObject config) {

		TestModuleHolder holder = getTestModules().get(testName);

		if (holder == null) {
			logger.warn("Couldn't find a test module for " + testName);
			return null;
		}

		try {

			Class<? extends TestModule> testModuleClass = holder.c;

			@SuppressWarnings("unchecked")
			Map<String, String> owner = (ImmutableMap<String, String>) authenticationFacade.getPrincipal();

			TestInstanceEventLog wrappedEventLog = new TestInstanceEventLog(id, owner, eventLog);

			BrowserControl browser = new BrowserControl(config, id, wrappedEventLog, executorCompletionService);

			// call the constructor
			TestModule module = testModuleClass.getDeclaredConstructor(String.class, Map.class, TestInstanceEventLog.class, BrowserControl.class, TestInfoService.class)
				.newInstance(id, owner, wrappedEventLog, browser, testInfo);
			return module;

		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {

			logger.warn("Couldn't create test module", e);

			return null;
		}

	}

	// get the test modules from the memoized copy, filling it if necessary
	private Map<String, TestModuleHolder> getTestModules() {
		return testModuleSupplier.get();
	}

	// this is used to load all the test modules into the memoized copy used above
	// we memoize this because reflection is slow
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

		public TestModuleHolder(Class<? extends TestModule> c, PublishTestModule a) {
			this.c = c;
			this.a = a;
		}
	}

	private Map<String, Object> createTestStatusMap(TestModule test) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", test.getName());
		map.put("id", test.getId());
		map.put("exposed", test.getExposedValues());
		map.put("owner", test.getOwner());
		map.put("created", test.getCreated().toString());
		map.put("updated", test.getStatusUpdated().toString());
		map.put("error", EventLog.ex(test.getFinalError()));

		BrowserControl browser = test.getBrowser();
		if (browser != null) {
			Map<String, Object> bmap = new HashMap<>();
			bmap.put("urls", browser.getUrls());
			bmap.put("visited", browser.getVisited());
			bmap.put("runners", browser.getWebRunners());
			map.put("browser", bmap);
		}
		return map;
	}

	// handle errors thrown by running tests
	@ExceptionHandler(TestFailureException.class)
	public ResponseEntity<Object> conditionFailure(TestFailureException error) {
		try {
			TestModule test = support.getRunningTestById(error.getTestId());
			if (test != null) {
				logger.error("Caught an error while running the test, stopping the test: " + error.getMessage());
				test.stop();
				eventLog.log(test.getId(), "TEST-RUNNER", test.getOwner(), EventLog.ex(error));
			}
			if (!(error.getCause() != null && error.getCause().getClass().equals(ConditionError.class))) {
				// if the root error isn't a ConditionError, set this so the UI can display the underlying error in detail
				// ConditionError will get handled by the logging system, no need to display with stacktrace
				test.setFinalError(error);
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
