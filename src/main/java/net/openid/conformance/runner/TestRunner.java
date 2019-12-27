package net.openid.conformance.runner;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.openid.conformance.testmodule.OIDFJSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.EventLog;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.TestInterruptedException;
import net.openid.conformance.testmodule.TestSkippedException;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.variant.VariantService;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.frontChannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.SavedConfigurationService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.info.TestPlanService;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.TestModule;

/**
 *
 * GET /api/runner/available: list of available tests
 * GET /api/runner/running: list of running tests
 * POST /api/runner: create test
 * GET /api/runner/id: get test status
 * POST /api/runner/id: start test
 * DELETE /api/runner/id: cancel test
 * GET /api/runner/browser/id: get front-channel external URLs
 * POST /api/runner/browser/id/visit: mark front-channel external URL as visited
 *
 */
@Controller
@Api(value = "A component that starts, stops, and manages running TestModules")
@RequestMapping(value = "/api")
public class TestRunner implements DataUtils {

	@Value("${fintechlabs.base_url:http://localhost:8080}")
	private String baseUrl;

	/**
	 * Override url for external URLs
	 *
	 * This conformance suite sometimes needs to make urls that are accessible externally, for example the CIBA
	 * ping/push notification endpoints. When the developer is running the suite locally tested an authorization
	 * server hosted in the cloud, the authorization server cannot directly reach the conformance suite, hence it
	 * is necessary to setup a relay and when dynamically registering a client we need to override the url.
	 *
	 * This setting should contain the external url that will be registered with the notification server.
	 * (If using statically created clients, this setting has no effect other than on the notification url
	 * displayed on the test detail page.)
	 *
	 * There are further notes in the wiki:
	 *
	 * https://gitlab.com/openid/conformance-suite/wikis/Developers/Build-&-Run#ciba-notification-endpoint
	 *
	 */
	@Value("${fintechlabs.external_url_override:}")
	public String externalUrlOverride;

	private static Logger logger = LoggerFactory.getLogger(TestRunner.class);

	@Autowired
	private TestRunnerSupport support;

	@Autowired
	private EventLog eventLog;

	@Autowired
	private TestInfoService testInfo;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private TestPlanService planService;

	@Autowired
	private ImageService imageService;

	@Autowired
	private SavedConfigurationService savedConfigurationService;

	@Autowired
	private VariantService variantService;

	private ExecutorService executorService = Executors.newCachedThreadPool();
	private ExecutorCompletionService<Object> executorCompletionService = new ExecutorCompletionService<>(executorService);
	private FutureWatcher futureWatcher = new FutureWatcher();

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
					Future<?> future = executorCompletionService.poll(1, TimeUnit.SECONDS);
					if (future != null && !future.isCancelled()) {
						future.get();
					}
				} catch (InterruptedException e) {
					// If we've been interrupted, then either it was on purpose, or something went very very wrong.
					logger.error("Background task was interrupted", e);
				} catch (ExecutionException e) {
					if (e.getCause() instanceof TestInterruptedException) {
						// This should always be the case for our BackgroundTasks
						TestInterruptedException testException = (TestInterruptedException) e.getCause();

						String testId = testException.getTestId();
						TestModule test = support.getRunningTestById(testId);
						if (test != null) {
							// We can't just throw it, the Exception Handler Annotation is only for HTTP requests
							conditionFailure(testException);
						}
					} else {
						// TODO: Better handling if we get something we wern't expecting? But we don't have access to the test ID
						logger.error("Execution failure", e);
						//eventLog.log(testId, "TEST RUNNER", authenticationFacade.getPrincipal(), EventLog.ex(e));
					}

				}
			}
		}
	}

	public TestRunner() {
		executorService.submit(futureWatcher);
	}

	@ApiOperation(value = "Get list of available TestModule names")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Retrieved successfully")
	})
	@RequestMapping(value = "/runner/available", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> getAvailableTests(Model m) {

		List<?> available = variantService.getTestModules().stream()
			.map(e -> args(
				"testName", e.info.testName(),
				"displayName", e.info.displayName(),
				"profile", e.info.profile(),
				"configurationFields", e.info.configurationFields(),
				"variants", e.getVariantSummary(),
				"summary", e.info.summary()))
			.collect(Collectors.toList());

		return new ResponseEntity<>(available, HttpStatus.OK);
	}

	@ApiOperation(value = "Create test - After a test is created, use /api/info/{testid} to wait for the test to be in the WAITING state before trying to interact with the test")
	@ApiResponses(value = {
		@ApiResponse(code = 201, message = "Created test successfully"),
		@ApiResponse(code = 400, message = "You shouldn't supply a configuration when creating a test from a test plan / You should supply a configuration when creating individual test module"),
		@ApiResponse(code = 404, message = "Couldn't find configuration of plan Id you provided"),
		@ApiResponse(code = 409, message = "There was a failure in creating the test alias"),
		@ApiResponse(code = 500, message = "Created test failed"),
	})
	@RequestMapping(value = "/runner", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> createTest(@ApiParam(value = "Test name, use to identify a specific TestModule") @RequestParam("test") String testName,
														  @ApiParam(value = "Plan Id") @RequestParam(name = "plan", required = false) String planId,
														  @ApiParam(value = "Kind of test variation") @RequestParam(name = "variant", required = false) VariantSelection variant,
														  @ApiParam(value = "Configuration for running test") @RequestBody(required = false) JsonObject testConfig,
														  Model m) {
		final JsonObject config;
		final VariantSelection testVariant;

		String id = RandomStringUtils.randomAlphanumeric(10);

		if (!Strings.isNullOrEmpty(planId)) {
			if (testConfig != null) {
				// user should not supply a configuration when creating a test from a test plan
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			// if the test is part of a plan, the configuration comes from the plan
			testVariant = planService.getTestPlanVariant(planId);
			config = planService.getModuleConfig(planId, testName);
			if (config == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} else {
			// we're starting an individual test module
			config = testConfig;
			testVariant = variant;
			if (config == null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			// save this test config on the user's stack
			savedConfigurationService.saveTestConfigurationForCurrentUser(config, testName, testVariant);

		}

		TestModule test = createTestModule(testName, id, config, testVariant);

		if (test == null) {
			// return an error
			return new ResponseEntity<>(stringMap("error", "createTestModule failed"), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		logger.info("Created: " + testName);

		// logger.info("Status of " + testName + ": " + test.getStatus());

		support.addRunningTest(id, test);

		String alias = "";
		String path;

		// see if an alias was passed in as part of the configuration and use it if available
		if (config.has("alias") && config.get("alias").isJsonPrimitive()) {
			{
				alias = OIDFJSON.getString(config.get("alias"));

				// create an alias for the test
				if (!createTestAlias(alias, id)) {
					// there was a failure in creating the test alias, return an error
					return new ResponseEntity<>(HttpStatus.CONFLICT);
				}
				path = TestDispatcher.TEST_PATH + "a/" + UriUtils.encodePathSegment(alias, "UTF-8");
			}

		} else {
			path = TestDispatcher.TEST_PATH + id;
		}
		String url = baseUrl + path;
		String externalOverrideUrlWithPath = Strings.isNullOrEmpty(externalUrlOverride) ? "" : externalUrlOverride + path;

		String description = null;
		if (config.has("description") && config.get("description").isJsonPrimitive()) {
			description = OIDFJSON.getString(config.get("description"));
		}

		// copy the summary from the test module
		String summary = variantService.getTestModule(testName).info.summary();

		// extract the `publish` field if available
		String publish = null;
		if (config.has("publish") && config.get("publish").isJsonPrimitive()) {
			publish = Strings.emptyToNull(OIDFJSON.getString(config.get("publish")));
		}

		// record that this test was started
		testInfo.createTest(id, testName, testVariant, url, config, alias, Instant.now(), planId, description, summary, publish);


		// log the test creation event in the event log
		eventLog.log(id, "TEST-RUNNER", test.getOwner(),
			args("msg", "Test instance " + id + " created",
				"result", Condition.ConditionResult.INFO,
				"baseUrl", url,
				"config", config,
				"alias", alias,
				"planId", planId,
				"description", description,
				"testName", testName,
				"variant", testVariant));

		test.getTestExecutionManager().runInBackground(() -> {
			test.configure(config, url, externalOverrideUrlWithPath);

			/* automatically start all tests */
			if (test.getStatus() == TestModule.Status.CONFIGURED) {
				test.start();
			}
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

				String message;
				if (TestModule.Status.FINISHED == test.getStatus() || TestModule.Status.INTERRUPTED == test.getStatus()) {
					message = "Alias has now been claimed by another test";
				} else {
					message = "Stopping test due to alias conflict";
				}
				eventLog.log(test.getId(), "TEST-RUNNER", test.getOwner(), args("msg", message, "alias", alias, "new_test_id", id));

				test.stop(); // stop the currently-running test
			}
		}

		support.addAlias(alias, id);
		return true;
	}

	@ApiOperation(value = "Start test by id")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Started test successfully"),
		@ApiResponse(code = 404, message = "The test you were trying to run is not found")
	})
	@RequestMapping(value = "/runner/{id}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> startTest(@ApiParam(value = "Id of test that you want to run") @PathVariable("id") String testId) {
		TestModule test = support.getRunningTestById(testId);
		if (test != null) {
			Map<String, Object> map = createTestStatusMap(test);

			//logger.info("Status of " + test.getName() + ": " + test.getId() + ": " + test.getStatus());

			test.getTestExecutionManager().runInBackground(() -> {
				test.start();
				return "started";
			});

			//logger.info("Status of " + test.getName() + ": " + test.getId() + ": " + test.getStatus());

			return ResponseEntity.ok().body(map);

		} else {
			return ResponseEntity.notFound().build();
		}

	}

	@ApiOperation(value = "Get test status, results, and exposed strings")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully"),
		@ApiResponse(code = 404, message = "The test you were trying to retrieve is not found")
	})
	@GetMapping(value = "/runner/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getTestStatus(@ApiParam(value = "Id of test that you want to get status") @PathVariable("id") String testId, Model m) {
		//logger.info("Getting status of " + testId);

		TestModule test = support.getRunningTestById(testId);
		if (test != null) {
			Map<String, Object> map = createTestStatusMap(test);

			return ResponseEntity.ok().body(map);

		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@ApiOperation(value = "Cancel test by Id")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Cancelled test successfully"),
		@ApiResponse(code = 404, message = "The test you were trying to cancel is not found")
	})
	@DeleteMapping(value = "/runner/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> cancelTest(@ApiParam(value = "Id of test that you want to cancel") @PathVariable("id") String testId) {
		// logger.info("Canceling " + testId);

		TestModule test = support.getRunningTestById(testId);
		if (test != null) {

			// stop the test
			test.getTestExecutionManager().runInBackground(() -> {
				eventLog.log(test.getId(), "TEST-RUNNER", test.getOwner(), args("msg", "Stopping test from external request"));
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

	@ApiOperation(value = "Get list of running testIDs")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Retrieved successfully")
	})
	@RequestMapping(value = "/runner/running", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Set<String>> getAllRunningTestIds(Model m) {
		Set<String> testIds = support.getAllRunningTestIds();

		return new ResponseEntity<>(testIds, HttpStatus.OK);
	}

	@ApiOperation(value = "Get front-channel external URLs exposed to the [BrowserControl] for a given test")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully"),
		@ApiResponse(code = 404, message = "The test you were trying to retrieve is not found"),
		@ApiResponse(code = 503, message = "Couldn't find Browser information")
	})
	@RequestMapping(value = "/runner/browser/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getBrowserStatus(@ApiParam(value = "Id of test") @PathVariable("id") String testId,
																Model m) {
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

	@ApiOperation(value = "Mark front-channel external URL as visited")
	@ApiResponses(value = {
		@ApiResponse(code = 204, message = "Visited url successfully"),
		@ApiResponse(code = 404, message = "The test you were trying to retrieve is not found"),
		@ApiResponse(code = 503, message = "Couldn't find Browser information")
	})
	@RequestMapping(value = "/runner/browser/{id}/visit", method = RequestMethod.POST)
	public ResponseEntity<String> visitBrowserUrl(@ApiParam(value = "Id of test") @PathVariable("id") String testId,
												  @ApiParam(value = "Url which you want to visit") @RequestParam("url") String url, Model m) {
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

	private TestModule createTestModule(String testName, String id, JsonObject config, VariantSelection variant) {

		VariantService.TestModuleHolder holder = variantService.getTestModule(testName);

		if (holder == null) {
			logger.warn("Couldn't find a test module for " + testName);
			return null;
		}

		try {

			Map<String, String> owner = authenticationFacade.getPrincipal();

			TestInstanceEventLog wrappedEventLog = new TestInstanceEventLog(id, owner, eventLog);

			TestExecutionManager executionManager = new TestExecutionManager(id, executorCompletionService, authenticationFacade);
			BrowserControl browser = new BrowserControl(config, id, wrappedEventLog, executionManager, imageService);

			TestModule module;

			// see if we're running a variant
			// in case, run test in the pipeline
			if (variant == null && config.has("variant")) {
				variant = VariantSelection.fromJson(config.get("variant"));
			}

			if (variant == null) {
				module = holder.newInstance(VariantSelection.EMPTY);
			} else {
				module = holder.newInstance(variant);
			}

			// pass in all the components for this test module to execute
			module.setProperties(id, owner, wrappedEventLog, browser, testInfo, executionManager, imageService);

			return module;

		} catch (IllegalArgumentException | SecurityException e) {

			logger.warn("Couldn't create test module", e);

			return null;
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
		map.put("error", ex(test.getFinalError()));

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
	@ExceptionHandler(TestInterruptedException.class)
	public ResponseEntity<Object> conditionFailure(TestInterruptedException error) {
		try {
			TestModule test = support.getRunningTestById(error.getTestId());
			if (test != null) {
				logger.error("Caught an error in TestRunner while running the test, stopping the test: " + error.getMessage());

				if (error.getCause() instanceof ConditionError) {
					Map<String, Object> event = new HashMap<>();
					event.put("msg", "The failure '"+error.getCause().getMessage()+"' means the test cannot continue. Stopping test.");
					eventLog.log(test.getId(), "TEST-RUNNER", test.getOwner(), ex(error, event));
				} else if (error instanceof TestSkippedException) {
					eventLog.log(test.getId(), "TEST-RUNNER", test.getOwner(),
							args(
								"result", TestModule.Result.SKIPPED,
								"msg", "The test was skipped: " + error.getMessage()));
				} else {
					eventLog.log(test.getId(), "TEST-RUNNER", test.getOwner(), ex(error));
				}

				if (error instanceof TestSkippedException) {
					// We don't need to do anything here
				} else {
					// Any other exception from a test counts as a failure
					test.fireTestFailure();
					test.stop();

					if (!(error.getCause() != null && error.getCause().getClass().equals(ConditionError.class))) {
						// if the root error isn't a ConditionError, set this so the UI can display the underlying error in detail
						// ConditionError will get handled by the logging system, no need to display with stacktrace
						test.setFinalError(error);
					}
				}
			} else {
				logger.error("Caught an error from a test, but the test isn't running: " + error.getMessage());
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
