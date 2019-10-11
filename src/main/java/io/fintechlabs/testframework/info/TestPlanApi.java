package io.fintechlabs.testframework.info;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.fintechlabs.testframework.testmodule.OIDFJSON;
import io.fintechlabs.testframework.variant.VariantSelection;
import io.fintechlabs.testframework.variant.VariantService;
import io.fintechlabs.testframework.variant.VariantService.TestPlanHolder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.pagination.PaginationRequest;
import io.fintechlabs.testframework.pagination.PaginationResponse;
import io.fintechlabs.testframework.testmodule.DataUtils;

@Controller
@RequestMapping(value = "/api")
public class TestPlanApi implements DataUtils {

	private static final Logger logger = LoggerFactory.getLogger(TestPlanApi.class);

	@Autowired
	private TestPlanService planService;

	@Autowired
	private SavedConfigurationService savedConfigurationService;

	@Autowired
	private VariantService variantService;

	@PostMapping(value = "/plan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Create test plan")
	@ApiResponses(value = {
		@ApiResponse(code = 201, message = "Created test plan successfully"),
		@ApiResponse(code = 404, message = "Couldn't find test plan for provided plan name")
	})
	public ResponseEntity<Map<String, Object>> createTestPlan(
		@ApiParam(value = "Plan name") @RequestParam("planName") String planName,
		@ApiParam(value = "Kind of test variation") @RequestParam(value = "variant", required = false) VariantSelection variant,
		@ApiParam(value = "Configuration json") @RequestBody JsonObject config,
		Model m) {

		String id = RandomStringUtils.randomAlphanumeric(13);

		TestPlanHolder holder = variantService.getTestPlan(planName);

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

		List<String> testModuleNames;
		if (variant != null) {
			testModuleNames = holder.getTestModulesForVariant(variant);
		} else {
			testModuleNames = holder.getTestModulesForVariant(VariantSelection.EMPTY);
		}

		if (testModuleNames.isEmpty()) {
			throw new RuntimeException("No test modules in plan '" + planName + "' are applicable for specified variant");
		}

		planService.createTestPlan(id, planName, variant, config, description, testModuleNames.toArray(new String[testModuleNames.size()]), holder.info.summary(), publish);

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
		TestPlanHolder holder = variantService.getTestPlan(planName);

		if (holder != null) {

			Map<String, ?> map = args(
					"planName", holder.info.testPlanName(),
					"displayName", holder.info.displayName(),
					"profile", holder.info.profile(),
					"moduleNames", holder.getTestModules(),
					"configurationFields", holder.info.configurationFields(),
					"summary", holder.info.summary());

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
		Set<Map<String, ?>> available = variantService.getTestPlans().stream()
			.map(e -> args(
				"planName", e.info.testPlanName(),
				"displayName", e.info.displayName(),
				"profile", e.info.profile(),
				"moduleNames", e.getTestModules(),
				"configurationFields", e.info.configurationFields(),
				"summary", e.info.summary(),
				"variants", e.getVariantSummary()
			))
			.collect(Collectors.toSet());

		return new ResponseEntity<>(available, HttpStatus.OK);
	}

}
