package net.openid.conformance.runner;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.vdurmont.semver4j.Semver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.frontchannel.BrowserControl;
import net.openid.conformance.info.ImageService;
import net.openid.conformance.info.Plan;
import net.openid.conformance.info.SavedConfigurationService;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.info.TestPlanService;
import net.openid.conformance.logging.EventLog;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestInterruptedException;
import net.openid.conformance.testmodule.TestModule;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
@Tag(name = "test-runner", description = "A component that starts, stops, and manages running TestModules")
@RequestMapping(value = "/api")
public class TestRunner implements DataUtils {

	@Value("${fintechlabs.base_url:http://localhost:8080}")
	private String baseUrl;

	@Value("${fintechlabs.base_mtls_url:http://localhost:8080}")
	private String baseMtlsUrl;

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

	private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);

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

		@SuppressWarnings("UnusedMethod") // Unsure why this was created but never called
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
							handleTestInterruptedException(testException, support, "TestRunner.java background task");
						} else {
							logger.error("Caught an exception for testId '"+testId+"' but it doesn't seem to be running", e);
						}
					} else {
						// There's not much more we can do as there's no way to get the testId; TestExecutionManager's
						// BackgroundTask does it's best to make sure we don't follow any of these paths.
						logger.error("Unexpected exception from future.get(): "+e.getMessage(), e);
					}

				}
			}
		}
	}

	@SuppressWarnings("FutureReturnValueIgnored")
	public TestRunner() {
		executorService.submit(futureWatcher);
	}

	@Operation(summary = "Get list of available TestModule names")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	@GetMapping(value = "/runner/available", produces = MediaType.APPLICATION_JSON_VALUE)
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

	@Operation(summary = "Create test module instance", description = "Normally a test plan should be created first. After a test is created, use /api/info/{testid} to wait for the test to be in the WAITING state before trying to interact with the test")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Created test successfully"),
		@ApiResponse(responseCode = "400", description = "You shouldn't supply a configuration when creating a test from a test plan / You should supply a configuration when creating individual test module"),
		@ApiResponse(responseCode = "404", description = "Couldn't find configuration of plan Id you provided"),
		@ApiResponse(responseCode = "409", description = "There was a failure in creating the test alias"),
		@ApiResponse(responseCode = "500", description = "Created test failed"),
	})
	@PostMapping(value = "/runner", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> createTest(@Parameter(description = "Test name, use to identify a specific TestModule") @RequestParam("test") String testName,
														  @Parameter(description = "Plan Id") @RequestParam(name = "plan", required = false) String planId,
														  @Parameter(description = "Kind of test variation") @RequestParam(name = "variant", required = false) VariantSelection variantFromApi,
														  @Parameter(description = "Configuration for running test") @RequestBody(required = false) JsonObject testConfig,
														  Model m) {
		final JsonObject config;
		final VariantSelection testVariant;
		Map<String, String> variantFromPlanDefinition = null;

		String id = RandomStringUtils.secure().nextAlphanumeric(15);

		if (!Strings.isNullOrEmpty(planId)) {
			if (testConfig != null) {
				// user should not supply a configuration when creating a test from a test plan
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			// stop if the plan is immutable
			Plan testPlan = planService.getTestPlan(planId);
			if(testPlan.getImmutable()!=null && testPlan.getImmutable()) {
				//the plan is immutable
				return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
			}
			Semver planVersion = new Semver(testPlan.getVersion());
			boolean recreate = false;
			if (testPlan.getPlanName().equals("fapi1-advanced-final-test-plan") ||
				testPlan.getPlanName().equals("fapi1-advanced-final-client-test-plan"))
			{
				if (planVersion.isLowerThan("5.1.11")) {
					recreate = true;
				}
			}
			if (testPlan.getPlanName().equals("fapi1-advanced-final-brazil-dcr-test-plan"))
			{
				if (planVersion.isLowerThan("5.1.11")) {
					recreate=true;
				}
			}
			if (testPlan.getPlanName().equals("fapi2-security-profile-id2-test-plan") ||
				testPlan.getPlanName().equals("fapi2-message-signing-id1-test-plan"))
			{
				if (planVersion.isLowerThan("5.1.1")) {
					recreate=true;
				}
			}
			if (recreate) {
				return new ResponseEntity<>(stringMap("error", "This test plan was created on an old version of the suite. Please recreate the plan (using the 'Edit Configuration' button)."), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			// if the test is part of a plan, the final variant may come from both any variants defined in the plan itself (which always take priority) combined with any selected by the user
			Map<String, String> variantsMap = new HashMap<>();
			if (variantFromApi == null) {
				Map<String, String> variant = getFixedVariantIfOnlyOneMatchingModuleInPlan(planId, testName);
				if (variant != null) {
					variantsMap.putAll(variant);
				}
			} else {
				variantsMap.putAll(variantFromApi.getVariant());
			}
			final VariantSelection testPlanVariant = planService.getTestPlanVariant(planId);
			if (testPlanVariant != null) {
				variantsMap.putAll(testPlanVariant.getVariant());
			}
			testVariant = new VariantSelection(variantsMap);
			config = planService.getModuleConfig(planId, testName);
			if (config == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			if (testPlanVariant != null) {
				// figure out which variants came from the plan definition (and hence are necessary to find the
				// right test module to update, if the module appears more than once in a plan with different
				// variants). We're tolerant of the API receiving variants we already know about, e.g. the UI
				// passes every variant when rerunning a test.
				// Worked out from the full set of variants, minus those the user selected for the plan
				variantFromPlanDefinition = new HashMap<>(variantsMap);
				for (String k : testPlanVariant.getVariant().keySet()) {
					variantFromPlanDefinition.remove(k);
				}
			}
		} else {
			// we're starting a test module that's not part of a plan
			config = testConfig;
			testVariant = variantFromApi;
			if (config == null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			// save this test config on the user's stack
			savedConfigurationService.saveTestConfigurationForCurrentUser(config, testName, testVariant);

		}

		TestModule test;
		try {
			test = createTestModule(testName, id, config, testVariant);
		} catch (IllegalArgumentException | SecurityException e) {

			logger.warn(id + ": Couldn't create test module", e);
			return new ResponseEntity<>(stringMap("error", "createTestModule failed: "+e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (test == null) {
			// return an error
			if (!Strings.isNullOrEmpty(planId)) {
				return new ResponseEntity<>(stringMap("error", "test module not found. The test module may have been renamed, if so recreating the test plan using the 'Edit Configuration' button may solve this."), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return new ResponseEntity<>(stringMap("error", "test module not found"), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		logger.info(id + ": Created: " + testName);

		// logger.info("Status of " + testName + ": " + test.getStatus());

		support.addRunningTest(id, test);

		String alias = "";
		String path;

		// see if an alias was passed in as part of the configuration and use it if available
		if (config.has("alias") && config.get("alias").isJsonPrimitive()) {

			alias = OIDFJSON.getString(config.get("alias"));

			try {
				// create an alias for the test
				createTestAlias(alias, id);
			} catch (Exception e) {
				// there was a failure in creating the test alias, return an error
				logger.info(id + ": " + testName + "createTestAlias failed: " + e.getMessage());
				support.removeRunningTest(id);
				return new ResponseEntity<>(stringMap("error", e.getMessage()), HttpStatus.CONFLICT);
			}
			path = "a/" + UriUtils.encodePathSegment(alias, "UTF-8");

		} else {
			path = id;
		}
		String url = baseUrl + TestDispatcher.TEST_PATH + path;
		String mtlsUrl = baseMtlsUrl + TestDispatcher.TEST_MTLS_PATH + path;
		String externalOverrideUrlWithPath = Strings.isNullOrEmpty(externalUrlOverride) ? "" : externalUrlOverride + TestDispatcher.TEST_PATH + path;

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
		VariantSelection variantFromPlanDefinitionObj = null;
		if (variantFromPlanDefinition != null) {
			variantFromPlanDefinitionObj = new VariantSelection(variantFromPlanDefinition);
		}
		testInfo.createTest(id, testName, testVariant, variantFromPlanDefinitionObj, url, config, alias, Instant.now(), planId, description, summary, publish);


		// log the test creation event in the event log
		eventLog.log(id, "TEST-RUNNER", test.getOwner(),
			args("msg", "Test instance " + id + " created",
				"result", Condition.ConditionResult.INFO,
				"baseUrl", url,
				"baseMtlsUrl", mtlsUrl,
				"config", config,
				"alias", alias,
				"planId", planId,
				"description", description,
				"testName", testName,
				"variant", testVariant));

		test.getTestExecutionManager().runInBackground(() -> {
			test.configure(config, url, externalOverrideUrlWithPath, mtlsUrl);

			if (test.getStatus() == TestModule.Status.CONFIGURED && test.autoStart()) {
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

	private Map<String, String> getFixedVariantIfOnlyOneMatchingModuleInPlan(String planId, String testName) {
		Plan plan = planService.getTestPlan(planId);
		List<Plan.Module> matchingModules = new ArrayList<>();
		for (var mod: plan.getModules()) {
			if (mod.getTestModule().equals(testName)) {
				matchingModules.add(mod);
			}
		}
		if (matchingModules.size() == 1) {
			return matchingModules.get(0).getVariant();
		}
		return null;
	}

	private void createTestAlias(String alias, String id) {
		// first see if the alias is already in use
		if (support.hasAlias(alias)) {
			// find the test that has the alias (even if it's owned by a different user)
			TestModule test = support.getRunningTestByAliasIgnoringLoggedInUser(alias);

			if (test != null) {
				boolean testHasStopped = TestModule.Status.FINISHED == test.getStatus() || TestModule.Status.INTERRUPTED == test.getStatus();
				boolean oldTestIsOwnedByCurrentUser = test.getOwner().equals(authenticationFacade.getPrincipal());

				if (authenticationFacade.isAdmin()) {
					// admin users are allowed to claim alias at any time
				} else {
					if (!oldTestIsOwnedByCurrentUser) {
						long idleTimeRequiredSeconds;
						if (testHasStopped) {
							// there are no tests running, but give the user a small grace window to start a new test
							idleTimeRequiredSeconds = 30;
						} else {
							// user is hopefully actively testing and a few of the tests have sleeps of many minutes
							idleTimeRequiredSeconds = 6 * 60;
						}
						long idleForSeconds = Duration.between(test.getStatusUpdated(), Instant.now()).toSeconds();
						if (idleForSeconds < idleTimeRequiredSeconds) {
							throw new RuntimeException("alias '" + alias + "' is in use by a different user. Please ensure you are using a unique alias value in your test configuration, for example by including the name of your company in it. If you are unable to change the alias you must wait until the other user has completed their testing. If the other user takes no further action the alias will be released in " + (idleTimeRequiredSeconds - idleForSeconds) + " seconds.");
						}
					}
				}

				String message;
				if (testHasStopped) {
					message = "Alias has now been claimed by another test";
				} else {
					message = "Stopping test due to alias conflict - before this test finished, ";
					if (oldTestIsOwnedByCurrentUser) {
						message += "you have ";
					} else {
						message += "another tester has ";
					}
					message += "started another test using the same alias. You will need to rerun this test and ensure you complete all steps in this test before you move onto the next test. Please check that the alias in your test configuration is unique, for example include your company name in it.";
				}
				eventLog.log(test.getId(), "TEST-RUNNER", test.getOwner(), args("msg", message, "alias", alias, "new_test_id", id));

				test.stop(message); // stop the currently-running test
			}
		}

		// there is a small race condition here if two users are trying to start a test at the same time; this method
		// should probably be inside a mutex
		support.addAlias(alias, id);
	}

	@Operation(summary = "Start test by id")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Started test successfully"),
		@ApiResponse(responseCode = "404", description = "The test you were trying to run is not found")
	})
	@PostMapping(value = "/runner/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> startTest(@Parameter(description = "Id of test that you want to run") @PathVariable("id") String testId) {
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

	@Operation(summary = "Get test status, results, and exposed strings")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "The test you were trying to retrieve is not found")
	})
	@GetMapping(value = "/runner/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getTestStatus(@Parameter(description = "Id of test that you want to get status") @PathVariable("id") String testId, Model m) {
		//logger.info("Getting status of " + testId);

		TestModule test = support.getRunningTestById(testId);
		if (test != null) {
			Map<String, Object> map = createTestStatusMap(test);

			return ResponseEntity.ok().body(map);

		} else {
			return ResponseEntity.notFound().build();
		}
	}

	@Operation(summary = "Cancel test by Id")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Cancelled test successfully"),
		@ApiResponse(responseCode = "404", description = "The test you were trying to cancel is not found")
	})
	@DeleteMapping(value = "/runner/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> cancelTest(@Parameter(description = "Id of test that you want to cancel") @PathVariable("id") String testId) {
		// logger.info("Canceling " + testId);

		TestModule test = support.getRunningTestById(testId);
		if (test != null) {

			// stop the test
			test.getTestExecutionManager().runInBackground(() -> {
				test.stop("The test was requested to stop via the conformance suite API.");
				return "stopped";
			});

			// return its immediate status
			Map<String, Object> map = createTestStatusMap(test);

			return new ResponseEntity<>(map, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@Operation(summary = "Get list of running testIDs")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	@GetMapping(value = "/runner/running", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Set<String>> getAllRunningTestIds(Model m) {
		Set<String> testIds = support.getAllRunningTestIds();

		return new ResponseEntity<>(testIds, HttpStatus.OK);
	}

	@Operation(summary = "Get front-channel external URLs exposed to the [BrowserControl] for a given test")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "The test you were trying to retrieve is not found"),
		@ApiResponse(responseCode = "503", description = "Couldn't find Browser information")
	})
	@GetMapping(value = "/runner/browser/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getBrowserStatus(@Parameter(description = "Id of test") @PathVariable("id") String testId,
																Model m) {
		// logger.info("Getting status of " + testId);

		TestModule test = support.getRunningTestById(testId);
		if (test != null) {
			BrowserControl browser = test.getBrowser();
			if (browser != null) {
				Map<String, Object> map = new HashMap<>();
				map.put("id", testId);
				map.put("show_qr_code", browser.showQrCodes());
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

	@Operation(summary = "Mark front-channel external URL as visited")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Visited url successfully"),
		@ApiResponse(responseCode = "404", description = "The test you were trying to retrieve is not found"),
		@ApiResponse(responseCode = "503", description = "Couldn't find Browser information")
	})
	@PostMapping("/runner/browser/{id}/visit")
	public ResponseEntity<String> visitBrowserUrl(@Parameter(description = "Id of test") @PathVariable("id") String testId,
												  @Parameter(description = "Url which you want to visit") @RequestParam String url, Model m) {
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
			logger.warn(id + ": Couldn't find a test module for " + testName);
			return null;
		}

		Map<String, String> owner = authenticationFacade.getPrincipal();

		TestInstanceEventLog wrappedEventLog = new TestInstanceEventLog(id, owner, eventLog);

		TestExecutionManager executionManager = new TestExecutionManager(id, executorCompletionService, authenticationFacade, support);
		BrowserControl browser = new BrowserControl(config, id, wrappedEventLog, executionManager, imageService);

		TestModule module;

		if (variant == null) {
			module = holder.newInstance(VariantSelection.EMPTY);
		} else {
			module = holder.newInstance(variant);
		}

		// pass in all the components for this test module to execute
		module.setProperties(id, owner, wrappedEventLog, browser, testInfo, executionManager, imageService);

		return module;

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
			bmap.put("show_qr_code", browser.showQrCodes());
			bmap.put("urls", browser.getUrls());
			bmap.put("urlsWithMethod", browser.getUrlsWithMethod());
			bmap.put("browserApiRequests", browser.getBrowserApiRequests());
			bmap.put("visited", browser.getVisited());
			bmap.put("visitedUrlsWithMethod", browser.getVisitedUrlsWithMethod());
			bmap.put("runners", browser.getWebRunners());
			map.put("browser", bmap);
		}
		return map;
	}

	// handle errors thrown by running tests
	@ExceptionHandler(TestInterruptedException.class)
	public ResponseEntity<Object> conditionFailure(TestInterruptedException error) {
		return handleTestInterruptedException(error, support, "TestRunner.java exception handler");
	}

	/**
	 * Handle an exception originating from a test
	 *
	 * @param error Exception that has been caught
	 * @param support The shared instance of TestRunnerSupport
	 * @param source Text explaining where the exception was caught, used only for logging
	 * @return An internal server error response containing JSON explaining the issue that occurred
	 */
	public static ResponseEntity<Object> handleTestInterruptedException(TestInterruptedException error,
																		TestRunnerSupport support,
																		String source) {
		try {
			TestModule test = support.getRunningTestById(error.getTestId());
			if (test != null) {
				test.handleException(error, source);
			} else {
				logger.error("Caught an exception in '"+source+"' for test '"+error.getTestId()+"', but the test doesn't seem to be running: " + error.getMessage(), error);
			}
		} catch (Exception e) {
			logger.error("Something terrible happened when handling an exception caught in '"+source+"', I give up", e);
		}

		HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
		JsonObject errorResponse = new JsonObject();

		if(error instanceof TestSkippedException) {
			statusCode = HttpStatus.OK;
		}

		if(error instanceof TestFailureException exception && exception.getError() != null) {
			errorResponse.addProperty("error", exception.getError());
			errorResponse.addProperty("error_description", exception.getErrorDescription());
			statusCode = HttpStatus.BAD_REQUEST;
		} else {
			errorResponse.addProperty("error", error.getMessage());
		}

		errorResponse.addProperty("cause", error.getCause() != null ? error.getCause().getMessage() : null);
		errorResponse.addProperty("testId", error.getTestId());
		return new ResponseEntity<>(errorResponse, statusCode);
	}
}
