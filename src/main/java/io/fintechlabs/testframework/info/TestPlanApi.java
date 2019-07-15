package io.fintechlabs.testframework.info;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.fintechlabs.testframework.testmodule.OIDFJSON;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.RandomStringUtils;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.pagination.PaginationRequest;
import io.fintechlabs.testframework.pagination.PaginationResponse;
import io.fintechlabs.testframework.plan.PublishTestPlan;
import io.fintechlabs.testframework.plan.TestPlan;
import io.fintechlabs.testframework.testmodule.DataUtils;

@Controller
@RequestMapping(value = "/api")
public class TestPlanApi implements DataUtils {

	private static final Logger logger = LoggerFactory.getLogger(TestPlanApi.class);

	private Supplier<Map<String, TestPlanHolder>> testPlanSupplier = Suppliers.memoize(this::findTestPlans);

	@Autowired
	private TestPlanService planService;

	@Autowired
	private SavedConfigurationService savedConfigurationService;

	@PostMapping(value = "/plan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Create test plan")
	@ApiResponses(value = {
		@ApiResponse(code = 201, message = "Created test plan successfully"),
		@ApiResponse(code = 404, message = "Couldn't find test plan for provided plan name")
	})
	public ResponseEntity<Map<String, Object>> createTestPlan(
		@ApiParam(value = "Plan name") @RequestParam("planName") String planName,
		@ApiParam(value = "Kind of test variation") @RequestParam(value = "variant", required = false) String variant,
		@ApiParam(value = "Configuration json") @RequestBody JsonObject config,
		Model m) {

		String id = RandomStringUtils.randomAlphanumeric(13);

		TestPlanHolder holder = getTestPlans().get(planName);

		if (holder == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		String description = null;
		if (config.has("description") && config.get("description").isJsonPrimitive()) {
			description = OIDFJSON.getString(config.get("description"));
		}

		// extract the `publish` field if available
		String publish = null;
		if (config.has("publish") && config.get("publish").isJsonPrimitive()) {
			publish = Strings.emptyToNull(OIDFJSON.getString(config.get("publish")));
		}

		// save the configuration for the test plan
		savedConfigurationService.savePlanConfigurationForCurrentUser(config, planName, variant);

		String[] testModuleNames = holder.testModuleNames;
		if (!Strings.isNullOrEmpty(variant)) {
			testModuleNames = holder.filterTestModule(variant);
		} else {
			// if a test plan has variants, the user must pick one
			if (holder.a.variants().length > 0) {
				throw new RuntimeException("Test plan '"+planName+"' has variants, configuration json must contain 'variant'");
			}
		}

		planService.createTestPlan(id, planName, variant, config, description, testModuleNames, holder.a.summary(), publish);

		Map<String, Object> map = new HashMap<>();
		map.put("name", planName);
		map.put("id", id);
		map.put("modules", testModuleNames);

		return new ResponseEntity<>(map, HttpStatus.CREATED);
	}

	@GetMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get a list of test plan with paging")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Retrieved successfully")
	})
	public ResponseEntity<Object> getTestPlansForCurrentUser(
		@ApiParam(value = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly,
		PaginationRequest page) {

		PaginationResponse<?> response = publicOnly
				? planService.getPaginatedPublicPlans(page)
				: planService.getPaginatedPlansForCurrentUser(page);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get test plan information by plan id")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully"),
		@ApiResponse(code = 404, message = "Couldn't find test plan for provided plan Id")
	})
	public ResponseEntity<Object> getTestPlan(
		@ApiParam(value = "Id of test plan") @PathVariable("id") String id,
		@ApiParam(value = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {

		Object testPlan = publicOnly
				? planService.getPublicPlan(id)
				: planService.getTestPlan(id);

		if (testPlan != null) {
			return new ResponseEntity<>(testPlan, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

	}

	@PostMapping(value = "/plan/{id}/publish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Publish a test plan by plan Id")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Published test plan successfully"),
		@ApiResponse(code = 400, message = "'public' field is missing or its value is not JsonPrimitive"),
		@ApiResponse(code = 403, message = "'publish' value is not valid or couldn't find test plan by provided plan Id")
	})
	public ResponseEntity<Object> publishTestPlan(@ApiParam(value = "Id of test plan that you want publish") @PathVariable("id") String id, @ApiParam(value = "Configuration Json") @RequestBody JsonObject config) {

		String publish = null;
		if (config.has("publish") && config.get("publish").isJsonPrimitive()) {
			publish = Strings.emptyToNull(OIDFJSON.getString(config.get("publish")));
		} else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (!planService.publishTestPlan(id, publish)) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		map.put("publish", publish);

		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@GetMapping(value = "plan/info/{planName}")
	@ApiOperation(value = "Get test plan information by plan name")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully"),
		@ApiResponse(code = 404, message = "Couldn't find test plan for provided plan name")
	})
	public ResponseEntity<Object> getTestPlanInfo(@ApiParam(value = "Plan name, use to identify a specific TestPlan ") @PathVariable("planName") String planName) {
		TestPlanHolder holder = getTestPlans().get(planName);

		if (holder != null) {

				Map map = args(
					"planName", holder.a.testPlanName(),
					"displayName", holder.a.displayName(),
					"profile", holder.a.profile(),
					"moduleNames", holder.testModuleNames,
					"configurationFields", holder.a.configurationFields(),
					"summary", holder.a.summary());

			return new ResponseEntity<>(map, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping(value = "plan/available")
	@ApiOperation(value = "Get a list of available test plan")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully")
	})
	public ResponseEntity<Object> getAvailableTestPlans() {
		Set<Map<String, ?>> available = getTestPlans()
			.values().stream()
			.map(e -> args(
				"planName", e.a.testPlanName(),
				"displayName", e.a.displayName(),
				"profile", e.a.profile(),
				"moduleNames", e.testModuleNames,
				"configurationFields", e.a.configurationFields(),
				"summary", e.a.summary(),
				"variants", Arrays.stream(e.a.variants())
					.map((v) -> args(
						"name", v
					)).collect(Collectors.toList())
			))
			.collect(Collectors.toSet());

		return new ResponseEntity<>(available, HttpStatus.OK);
	}

	private Map<String, TestPlanHolder> getTestPlans() {
		return testPlanSupplier.get();
	}

	private Map<String, TestPlanHolder> findTestPlans() {
		Map<String, TestPlanHolder> testPlans = new HashMap<>();

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(PublishTestPlan.class));
		for (BeanDefinition bd : scanner.findCandidateComponents("io.fintechlabs")) {
			try {
				@SuppressWarnings("unchecked")
				Class<? extends TestPlan> c = (Class<? extends TestPlan>) Class.forName(bd.getBeanClassName());
				PublishTestPlan a = c.getDeclaredAnnotation(PublishTestPlan.class);

				testPlans.put(a.testPlanName(), new TestPlanHolder(c, a));

			} catch (ClassNotFoundException e) {
				logger.error("Couldn't load test module definition: " + bd.getBeanClassName());
			}
		}

		return testPlans;

	}

	private class TestPlanHolder {
		public Class<? extends TestPlan> c;
		public PublishTestPlan a;
		public String[] testModuleNames;

		public TestPlanHolder(Class<? extends TestPlan> c, PublishTestPlan a) {
			this.c = c;
			this.a = a;

			if (a.testModules().length > 0) {
				this.testModuleNames = Arrays.stream(a.testModules())
					.map((m) -> m.getDeclaredAnnotation(PublishTestModule.class).testName())
					.toArray(String[]::new);
			}
		}

		public String[] filterTestModule(String variant) {
			if (a.testModules().length > 0) {
				return Arrays.stream(a.testModules())
					.filter(test -> {
						boolean isExist = Arrays.stream(test.getDeclaredMethods())
							.filter((m) -> m.isAnnotationPresent(Variant.class))
							.map((m) -> m.getDeclaredAnnotation(Variant.class).name())
							.collect(Collectors.toList())
							.contains(variant);
						if (!isExist) {
							if (Arrays.stream(test.getDeclaredAnnotation(PublishTestModule.class).notApplicableForVariants()).anyMatch(v -> v.equals(variant))) {
								return false;
							}
							String testModuleName = test.getDeclaredAnnotation(PublishTestModule.class).testName();
							String testPlanName = a.testPlanName();
							throw new RuntimeException("Variant '"+variant+"' not found in test module '"+testModuleName+"' of test plan '"+testPlanName+"'");
						}
						return true;
					})
					.map(test -> test.getDeclaredAnnotation(PublishTestModule.class).testName())
					.toArray(String[]::new);
			} else {
				return this.testModuleNames;
			}
		}
	}

}
