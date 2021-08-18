package net.openid.conformance.info;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.openid.conformance.CollapsingGsonHttpMessageConverter;
import net.openid.conformance.pagination.PaginationRequest;
import net.openid.conformance.pagination.PaginationResponse;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.variant.VariantService;
import org.apache.commons.lang3.RandomStringUtils;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/api")
public class TestPlanApi implements DataUtils {

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

		VariantService.TestPlanHolder holder = variantService.getTestPlan(planName);

		if (holder == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		String description = null;
		if (config.has("description") && config.get("description").isJsonPrimitive()) {
			description = OIDFJSON.getString(config.get("description"));
		}

		if (config.has("alias") && config.get("alias").isJsonPrimitive()) {
			String alias = Strings.emptyToNull(OIDFJSON.getString(config.get("alias")));
			if(!alias.matches("^([a-zA-Z0-9_-]+)$")) {
				throw new RuntimeException("Invalid alias value '" +alias+ "'. " +
					"alias can only contain alphanumeric characters, _ and -.");
			}
		}

		// extract the `publish` field if available
		String publish = null;
		if (config.has("publish") && config.get("publish").isJsonPrimitive()) {
			publish = Strings.emptyToNull(OIDFJSON.getString(config.get("publish")));
		}

		// save the configuration for the test plan
		savedConfigurationService.savePlanConfigurationForCurrentUser(config, planName, variant);

		List<Plan.Module> testModules;
		if (variant != null) {
			testModules = holder.getTestModulesForVariant(variant);
		} else {
			testModules = holder.getTestModulesForVariant(VariantSelection.EMPTY);
		}

		if (testModules.isEmpty()) {
			throw new RuntimeException("No test modules in plan '" + planName + "' are applicable for specified variant");
		}

		String certProfile = holder.certificationProfileForVariant(variant);

		planService.createTestPlan(id, planName, variant, config, description, certProfile, testModules, holder.info.summary(), publish);

		Map<String, Object> map = new HashMap<>();
		map.put("name", planName);
		map.put("id", id);
		map.put("modules", testModules);

		return new ResponseEntity<>(map, HttpStatus.CREATED);
	}

	@GetMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get a list of test plan instances with paging")
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

		Object testPlan = publicOnly ? planService.getPublicPlan(id) : planService.getTestPlan(id);

		if (testPlan == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Gson gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson();
		JsonObject testPlanObj = new JsonParser().parse(gson.toJson(testPlan)).getAsJsonObject();

		JsonElement modules = testPlanObj.get("modules");

		if (modules != null && modules.isJsonArray()) {
			((JsonArray) modules).forEach(m -> {
				String testModuleName = OIDFJSON.getString(m.getAsJsonObject().get("testModule"));
				VariantService.TestModuleHolder testModule = variantService.getTestModule(testModuleName);
				if (testModule != null) {
					m.getAsJsonObject().addProperty("testSummary", testModule.info.summary());
				}
			});
		}

		return new ResponseEntity<>(testPlanObj, HttpStatus.OK);
	}

	@PostMapping(value = "/plan/{id}/publish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Publish a test plan by plan Id")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Published test plan successfully"),
		@ApiResponse(code = 400, message = "'publish' field is missing or its value is not JsonPrimitive"),
		@ApiResponse(code = 403, message = "'publish' value is not valid or couldn't find test plan by provided plan Id")
	})
	public ResponseEntity<Object> publishTestPlan(@ApiParam(value = "Id of test plan that you want publish") @PathVariable("id") String id,
												  @ApiParam(value = "Configuration Json") @RequestBody JsonObject config) {

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

	@PostMapping(value = "/plan/{id}/makemutable", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
	@ApiOperation(value = "Make a test plan mutable again (requires administrator privileges)")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Made the test plan mutable again successfully"),
		@ApiResponse(code = 400, message = "Could not find plan"),
		@ApiResponse(code = 403, message = "Not authorized")
	})
	public ResponseEntity<Object> makeTestPlanMutable(@ApiParam(value = "Id of test plan that you want make mutable again") @PathVariable("id") String id) {
		if (!planService.changeTestPlanImmutableStatus(id, Boolean.FALSE)) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "plan/info/{planName}")
	@ApiOperation(value = "Get information for one test plan by name")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully"),
		@ApiResponse(code = 404, message = "Couldn't find test plan for provided plan name")
	})
	public ResponseEntity<Object> getTestPlanInfo(@ApiParam(value = "Plan name, use to identify a specific TestPlan ") @PathVariable("planName") String planName) {
		VariantService.TestPlanHolder holder = variantService.getTestPlan(planName);

		if (holder != null) {

			Map<String, ?> map = args(
					"planName", holder.info.testPlanName(),
					"displayName", holder.info.displayName(),
					"profile", holder.info.profile(),
					"modules", holder.getTestModules(),
					"configurationFields", holder.configurationFields(),
					"hidesConfigurationFields", holder.hidesConfigurationFields(),
					"summary", holder.info.summary(),
					"variants", holder.getVariantSummary()
			);

			return new ResponseEntity<>(map, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping(value = "plan/available")
	@ApiOperation(value = "Get a list of available test plans and their attributes")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully")
	})
	public ResponseEntity<Object> getAvailableTestPlans() {
		Set<Map<String, ?>> available = variantService.getTestPlans().stream()
			.map(e -> args(
				"planName", e.info.testPlanName(),
				"displayName", e.info.displayName(),
				"profile", e.info.profile(),
				"modules", e.getTestModules(),
				"configurationFields", e.configurationFields(),
				"hidesConfigurationFields", e.hidesConfigurationFields(),
				"summary", e.info.summary(),
				"variants", e.getVariantSummary()
			))
			.collect(Collectors.toSet());

		return new ResponseEntity<>(available, HttpStatus.OK);
	}

}
