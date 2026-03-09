package net.openid.conformance.info;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import net.openid.conformance.CollapsingGsonHttpMessageConverter;
import net.openid.conformance.pagination.PaginationRequest;
import net.openid.conformance.pagination.PaginationResponse;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.sharing.AssetSharing;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.variant.VariantService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.ArrayList;
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
	private TestInfoService infoService;

	@Autowired
	private SavedConfigurationService savedConfigurationService;

	@Autowired
	private VariantService variantService;

	@Autowired
	@SuppressWarnings("unused")
	private AssetSharing assetSharing;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Value("${fintechlabs.base_url}")
	private String baseURL;

	@PostMapping(value = "/plan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Create test plan")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Created test plan successfully"),
		@ApiResponse(responseCode = "400", description = "Unknown variant parameter(s) for the plan"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions to create test plan"),
		@ApiResponse(responseCode = "404", description = "Couldn't find test plan for provided plan name")
	})
	public ResponseEntity<Map<String, Object>> createTestPlan(
		@Parameter(description = "Plan name") @RequestParam String planName,
		@Parameter(description = "Kind of test variation") @RequestParam(required = false) VariantSelection variant,
		@Parameter(description = "Configuration json") @RequestBody JsonObject config,
		Model m) {

		if (authenticationFacade.isPrivateLinkUser()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		String id = RandomStringUtils.secure().nextAlphanumeric(13);

		VariantService.TestPlanHolder holder = variantService.getTestPlan(planName);

		if (holder == null) {
			return new ResponseEntity<>(Map.of("error", "No plan with name: "+planName+""), HttpStatus.NOT_FOUND);
		}

		String description = null;
		if (config.has("description") && config.get("description").isJsonPrimitive()) {
			description = OIDFJSON.getString(config.get("description"));
		}

		if (config.has("alias") && config.get("alias").isJsonPrimitive()) {
			String alias = Strings.emptyToNull(OIDFJSON.getString(config.get("alias")));
			if(!alias.matches("^([a-zA-Z0-9_-]+)$")) {
				Map<String, Object> map = new HashMap<>();
				map.put("error", "Invalid alias value '" +alias+ "'. " +
						"alias can only contain alphanumeric characters, _ and -.");
				return new ResponseEntity<>(map, HttpStatus.BAD_REQUEST);
			}
		}

		// extract the `publish` field if available
		String publish = null;
		if (config.has("publish") && config.get("publish").isJsonPrimitive()) {
			publish = Strings.emptyToNull(OIDFJSON.getString(config.get("publish")));
		}

		// Reject any variant parameters not recognized by any module in the plan
		if (variant != null) {
			Set<String> known = holder.getKnownParameterNames();
			Set<String> unknown = variant.getVariant().keySet().stream()
				.filter(k -> !known.contains(k))
				.collect(Collectors.toSet());
			if (!unknown.isEmpty()) {
				return new ResponseEntity<>(
					Map.of("error", "Unknown variant parameter(s) " + unknown
						+ " for plan '" + planName + "'. Known parameters: " + known),
					HttpStatus.BAD_REQUEST);
			}
		}

		// Resolve default values for any unset variant parameters so the stored
		// plan records exactly what ran (needed for edit-plan UI restoration).
		// getUnsetDefaults only returns keys not already in the user variant,
		// so putAll will not overwrite any user-provided values.
		if (variant != null) {
			Map<String, String> resolved = new HashMap<>(variant.getVariant());
			resolved.putAll(holder.getUnsetDefaults(variant));
			variant = new VariantSelection(resolved);
		}

		// save the configuration for the test plan
		savedConfigurationService.savePlanConfigurationForCurrentUser(config, planName, variant);

		List<Plan.Module> testModules;
		try {
			if (variant != null) {
				testModules = holder.getTestModulesForVariant(variant);
			} else {
				testModules = holder.getTestModulesForVariant(VariantSelection.EMPTY);
			}
		} catch (RuntimeException e) {
			return new ResponseEntity<>(
				Map.of("error", e.getMessage()),
				HttpStatus.BAD_REQUEST);
		}

		if (testModules.isEmpty()) {
			return new ResponseEntity<>(
				Map.of("error", "No test modules in plan '" + planName + "' are applicable for specified variant"),
				HttpStatus.BAD_REQUEST);
		}

		List<String> certProfile = holder.certificationProfileForVariant(variant);

		planService.createTestPlan(id, planName, variant, config, description, certProfile, testModules, holder.info.summary(), publish);

		Map<String, Object> map = new HashMap<>();
		map.put("name", planName);
		map.put("id", id);
		map.put("modules", testModules);

		return new ResponseEntity<>(map, HttpStatus.CREATED);
	}

	@GetMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get a list of test plan instances with paging")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<Object> getTestPlansForCurrentUser(
		@Parameter(description = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly,
		PaginationRequest page) {

		PaginationResponse<?> response = publicOnly
				? planService.getPaginatedPublicPlans(page)
				: planService.getPaginatedPlansForCurrentUser(page);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/plan/{id}/share")
	@Operation(summary = "Get private link to share test plan")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions to share plan"),
		@ApiResponse(responseCode = "404", description = "Couldn't find test plan for provided plan Id")
	})
	public ResponseEntity<?> shareLink(
		@Parameter(description = "Id of test plan") @PathVariable String id,
		@Parameter(description = "Link expiry days") @RequestParam(name = "exp", required = true) String exp
	) {

		if (authenticationFacade.isPrivateLinkUser()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		Plan testPlan = planService.getTestPlan(id);

		if (testPlan == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		OneTimeToken oneTimeToken = assetSharing.generateSharingToken(id, testPlan.getOwner(), exp);

		return ResponseEntity.ok().body(Map.of("link", baseURL + "/login.html?token=" + oneTimeToken.getTokenValue(),
							"message", assetSharing.generateSharingTokenSupplementalMessage()));
	}

	@GetMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get test plan information by plan id")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Couldn't find test plan for provided plan Id")
	})
	public ResponseEntity<Object> getTestPlan(
		@Parameter(description = "Id of test plan") @PathVariable String id,
		@Parameter(description = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly) {

		Object testPlan = publicOnly ? planService.getPublicPlan(id) : planService.getTestPlan(id);

		if (testPlan == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Gson gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson();
		JsonObject testPlanObj = JsonParser.parseString(gson.toJson(testPlan)).getAsJsonObject();

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

		// FIXME: Remove workaround for dangling testplan configurations and implement explicit support for testplan migrations
		if (testPlanObj.has("planName")) {
			String planName = OIDFJSON.getString(testPlanObj.get("planName"));
			Map<String, String> map = getOldTestPlanToNewTestPlanMap();
			// rename testplan if necessary
			if (map.containsKey(planName)) {
				String newPlanName = map.get(planName);
				testPlanObj.addProperty("planName", newPlanName);
			}
		}

		return new ResponseEntity<>(testPlanObj, HttpStatus.OK);
	}

	/**
	 * Maps old testplan names to new testplan names to retain configurations.
	 * @return
	 */
	protected Map<String, String> getOldTestPlanToNewTestPlanMap() {
		return Map.ofEntries(
			Map.entry("oid4vci-id2-issuer-test-plan", "oid4vci-1_0-issuer-test-plan"),
			Map.entry("oid4vci-id2-wallet-test-plan", "oid4vci-1_0-wallet-test-plan")
		);
	}

	@PostMapping(value = "/plan/{id}/publish", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Publish a test plan by plan Id")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Published test plan successfully"),
		@ApiResponse(responseCode = "400", description = "'publish' field is missing or its value is not JsonPrimitive"),
		@ApiResponse(responseCode = "403", description = "'publish' value is not valid or couldn't find test plan by provided plan Id")
	})
	public ResponseEntity<Object> publishTestPlan(@Parameter(description = "Id of test plan that you want publish") @PathVariable String id,
												  @Parameter(description = "Configuration Json") @RequestBody JsonObject config) {

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
	@Operation(summary = "Make a test plan mutable again (requires administrator privileges)")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Made the test plan mutable again successfully"),
		@ApiResponse(responseCode = "400", description = "Could not find plan"),
		@ApiResponse(responseCode = "403", description = "Not authorized")
	})
	public ResponseEntity<Object> makeTestPlanMutable(
			@Parameter(description = "Id of test plan that you want make mutable again") @PathVariable String id) {
		if (!planService.changeTestPlanImmutableStatus(id, Boolean.FALSE)) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "plan/info/{planName}")
	@Operation(summary = "Get information for one test plan by name")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Couldn't find test plan for provided plan name")
	})
	public ResponseEntity<Object> getTestPlanInfo(
			@Parameter(description = "Plan name, use to identify a specific TestPlan ") @PathVariable String planName) {
		VariantService.TestPlanHolder holder = variantService.getTestPlan(planName);

		if (holder != null) {

			Map<String, ?> map = args(
					"planName", holder.info.testPlanName(),
					"displayName", holder.info.displayName(),
					"profile", holder.info.profile(),
					"specFamily", holder.info.specFamily(),
					"specVersion", holder.info.specVersion(),
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
	@Operation(summary = "Get a list of available test plans and their attributes")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<Object> getAvailableTestPlans() {
		Set<Map<String, ?>> available = variantService.getTestPlans().stream()
			.<Map<String, ?>>map(e -> args(
				"planName", e.info.testPlanName(),
				"displayName", e.info.displayName(),
				"profile", e.info.profile(),
				"specFamily", e.info.specFamily(),
				"specVersion", e.info.specVersion(),
				"modules", e.getTestModulesWithConfigFields(),
				"configurationFields", e.configurationFields(),
				"hidesConfigurationFields", e.hidesConfigurationFields(),
				"summary", e.info.summary(),
				"variants", e.getVariantSummary()
			))
			.collect(Collectors.toSet());

		return new ResponseEntity<>(available, HttpStatus.OK);
	}

	@DeleteMapping(value = "/plan/{id}")
	@Operation(summary = "Delete a test plan and related configuration. Requires the plan to be mutable.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Deleted successfully"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions to delete test plan"),
		@ApiResponse(responseCode = "404", description = "Could not find a plan with the given id, belonging to the user"),
		@ApiResponse(responseCode = "405", description = "The plan is immutable and cannot be deleted")
	})
	public ResponseEntity<StreamingResponseBody> deleteMutableTestPlan(
		@Parameter(description = "Id of test plan") @PathVariable String id
	) {
		if (authenticationFacade != null && authenticationFacade.isPrivateLinkUser()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		Plan testPlan = planService.getTestPlan(id);
		if(testPlan == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if(testPlan.getImmutable() != null && testPlan.getImmutable()) {
			return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
		}

		List<String> testIds = testPlan.getModules().stream().map(Plan.Module::getInstances).collect(ArrayList::new, List::addAll, List::addAll);
		infoService.deleteTests(testIds);
		planService.deleteMutableTestPlan(id);

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

}
