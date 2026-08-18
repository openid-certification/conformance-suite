package net.openid.conformance.info;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.CollapsingGsonHttpMessageConverter;
import net.openid.conformance.SwaggerConfig;
import net.openid.conformance.apidoc.ErrorResponse;
import net.openid.conformance.apidoc.PlanCreatedResponse;
import net.openid.conformance.apidoc.PublishResponse;
import net.openid.conformance.apidoc.ShareLinkResponse;
import net.openid.conformance.pagination.PaginationRequest;
import net.openid.conformance.pagination.PaginationResponse;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.sharing.AssetSharing;
import net.openid.conformance.statistics.QueryParams;
import net.openid.conformance.statistics.SpecFamilyResolver;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.variant.VariantSelection;
import net.openid.conformance.variant.VariantService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
@Tag(name = SwaggerConfig.TAG_TEST_PLANS)
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
	private AssetSharing assetSharing;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private SpecFamilyResolver specFamilyResolver;

	@Autowired
	private BulkPlanDeleter bulkPlanDeleter;

	/** {@link #filterOptions()}, built on first use; the registry it describes never changes. */
	private volatile Map<String, Object> planFilterOptions;

	@PostMapping(value = "/plan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "createTestPlan", summary = "Create test plan")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Created test plan successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlanCreatedResponse.class))),
		@ApiResponse(responseCode = "400", description = "Unknown variant parameter(s), invalid alias, or no applicable test modules for the variant",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions to create test plan", content = @Content),
		@ApiResponse(responseCode = "404", description = "Couldn't find test plan for provided plan name",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})
	public ResponseEntity<Map<String, Object>> createTestPlan(
		@Parameter(description = "Plan name") @RequestParam String planName,
		@Parameter(description = SwaggerConfig.DESC_VARIANT_SELECTION, example = SwaggerConfig.EXAMPLE_VARIANT_SELECTION) @RequestParam(required = false) VariantSelection variant,
		@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The test configuration JSON; may include 'description', 'alias' and 'publish' fields alongside the server/client configuration",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(SwaggerConfig.EXAMPLE_TEST_CONFIGURATION)))
		@RequestBody JsonObject config,
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
	@Operation(operationId = "listTestPlans", summary = "Get a list of test plan instances with paging",
		description = "Which plans are listed is decided by who is asking: an admin sees every plan, "
			+ "anyone else sees their own, and `public=true` lists the published plans. The optional "
			+ "filters below only narrow that listing - the statistics page drills down with them - so "
			+ "none of them can show a plan that would not have been listed anyway.\n\n"
			+ "In addition to the parameters below, any number of plan level variant filters may be sent "
			+ "as `variant.<parameter>=<value>`, e.g. "
			+ "`variant.fapi_profile=openbanking_brazil&variant.client_auth_type=mtls`; a plan has to "
			+ "match all of them. They cannot be declared individually here because the parameter names "
			+ "are the variant parameters of every test plan the suite publishes.")
	@Parameters({
		@Parameter(name = "family", in = ParameterIn.QUERY,
			description = "Only list plans of this spec family, as named on the statistics page.",
			schema = @Schema(type = "string", example = "FAPI-CIBA")),
		@Parameter(name = "plan", in = ParameterIn.QUERY,
			description = "Only list plans with this exact plan name.",
			schema = @Schema(type = "string", example = "fapi-ciba-id1-test-plan")),
		@Parameter(name = "cert", in = ParameterIn.QUERY,
			description = "Only list plans certified against this certification profile; a plan matches "
				+ "if any one of its profiles is exactly this.",
			schema = @Schema(type = "string", example = "FAPI-CIBA: Poll w/ MTLS")),
		@Parameter(name = "owner", in = ParameterIn.QUERY,
			description = "Only list plans belonging to this user, by the `sub` of their account. Has "
				+ "to be sent with `owner_iss`, since a `sub` names an account only within the issuer "
				+ "that minted it. Narrows the listing and can never widen it: anyone but an admin "
				+ "still sees only their own plans, so naming another owner lists nothing.",
			schema = @Schema(type = "string")),
		@Parameter(name = "owner_iss", in = ParameterIn.QUERY,
			description = "The issuer that minted the `owner` sub, as the account was logged in with.",
			schema = @Schema(type = "string", example = "https://accounts.google.com")),
		@Parameter(name = "immutable", in = ParameterIn.QUERY,
			description = "Only list plans a certification package has been downloaded for (`true`), "
				+ "or only those it has not (`false`). Omit for both.",
			schema = @Schema(type = "boolean")),
		@Parameter(name = "from", in = ParameterIn.QUERY,
			description = "Only list plans started at or after this point in time; a date (`YYYY-MM-DD`, "
				+ "covering the whole of that day) or a timestamp with a time zone.",
			schema = @Schema(type = "string", example = "2026-06-01")),
		@Parameter(name = "to", in = ParameterIn.QUERY,
			description = "Only list plans started before this point in time, exclusive, in the same "
				+ "format as `from`, so that the bounds of adjacent periods can be passed straight through.",
			schema = @Schema(type = "string", example = "2026-07-01"))
	})
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully; 'data' contains test plan documents (the public projection when public=true)",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PaginationResponse.class))),
		@ApiResponse(responseCode = "400", description = "A filter parameter could not be used",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
	})
	public ResponseEntity<Object> getTestPlansForCurrentUser(
		@Parameter(description = "Published data only") @RequestParam(name = "public", defaultValue = "false") boolean publicOnly,
		@ParameterObject PaginationRequest page,
		// the filters are read from the raw parameter map because variant.<parameter> is an
		// open set of names that no @RequestParam can declare
		@Parameter(hidden = true) HttpServletRequest request) {

		PlanListFilter filter;
		PlanOwner owner;
		try {
			filter = PlanListFilter.parse(request.getParameterMap(), specFamilyResolver);
			// not part of the filter: owner is what scopes a listing, which a filter may never
			// touch, so DBTestPlanService applies it to the scope instead
			owner = PlanOwner.parse(request.getParameterMap());
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
		}

		PaginationResponse<?> response = publicOnly
				? planService.getPaginatedPublicPlans(page, filter, owner)
				: planService.getPaginatedPlansForCurrentUser(page, filter, owner);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/plan/{id}/share")
	@Operation(operationId = "shareTestPlan", summary = "Get private link to share test plan",
		description = SwaggerConfig.DESC_SHARE_LINK)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ShareLinkResponse.class))),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions to share plan", content = @Content),
		@ApiResponse(responseCode = "404", description = "Couldn't find test plan for provided plan Id", content = @Content)
	})
	public ResponseEntity<?> shareLink(
		@Parameter(description = "Id of test plan") @PathVariable String id,
		@Parameter(description = SwaggerConfig.DESC_SHARE_EXPIRY, example = "30") @RequestParam(name = "exp", required = true) String exp
	) {

		if (authenticationFacade.isPrivateLinkUser()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		Plan testPlan = planService.getTestPlan(id);

		if (testPlan == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		return ResponseEntity.ok().body(assetSharing.generateShareLink(id, testPlan.getOwner(), exp));
	}

	@GetMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getTestPlan", summary = "Get test plan information by plan id",
		description = "Returns the stored plan document (a reduced public projection when public=true): planName, variant, config, started, owner, description, certificationProfileName, modules, version, summary, publish, immutable. Each modules[] entry additionally carries a 'testSummary' of its test module.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", description = "The plan document"))),
		@ApiResponse(responseCode = "404", description = "Couldn't find test plan for provided plan Id", content = @Content)
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

		JsonElement configEl = testPlanObj.get("config");
		if (configEl != null && configEl.isJsonObject()) {
			ConfigMigration.migrateLegacyClientAttestationKeys(configEl.getAsJsonObject());
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
	@Operation(operationId = "publishTestPlan", summary = "Publish a test plan by plan Id")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Published test plan successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PublishResponse.class))),
		@ApiResponse(responseCode = "400", description = "'publish' field is missing or its value is not JsonPrimitive", content = @Content),
		@ApiResponse(responseCode = "403", description = "'publish' value is not valid or couldn't find test plan by provided plan Id", content = @Content)
	})
	public ResponseEntity<Object> publishTestPlan(@Parameter(description = "Id of the test plan to publish") @PathVariable String id,
												  @io.swagger.v3.oas.annotations.parameters.RequestBody(description = SwaggerConfig.DESC_PUBLISH_BODY,
													  content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(SwaggerConfig.EXAMPLE_PUBLISH_BODY)))
												  @RequestBody JsonObject config) {

		if (authenticationFacade.isPrivateLinkUser()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

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

	@PostMapping(value = "/plan/{id}/makemutable", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@Operation(operationId = "makeTestPlanMutable", summary = "Make a test plan mutable again (requires administrator privileges)")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Made the test plan mutable again successfully", content = @Content),
		@ApiResponse(responseCode = "403", description = "Not authorized, or the plan could not be found", content = @Content)
	})
	public ResponseEntity<Object> makeTestPlanMutable(
			@Parameter(description = "Id of the test plan to make mutable again") @PathVariable String id) {
		if (authenticationFacade.isPrivateLinkUser()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		if (!planService.changeTestPlanImmutableStatus(id, Boolean.FALSE)) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "plan/info/{planName}")
	@Operation(operationId = "getTestPlanInfo", summary = "Get information for one test plan by name")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object", description = "Plan definition: planName, displayName, profile, specFamily, specVersion, modules, configurationFields, hidesConfigurationFields, summary, variants"))),
		@ApiResponse(responseCode = "404", description = "Couldn't find test plan for provided plan name", content = @Content)
	})
	public ResponseEntity<Object> getTestPlanInfo(
			@Parameter(description = "Plan name, used to identify a specific test plan") @PathVariable String planName) {
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
	@Operation(operationId = "listAvailableTestPlans", summary = "Get a list of available test plans and their attributes")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "array", description = "One entry per available plan, shaped as GET /api/plan/info/{planName} except each module entry also lists its configurationFields")))
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

	// a literal path, like /plan/delete-status: no plan id can contain a hyphen
	@GetMapping(value = "/plan/filter-options", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getPlanFilterOptions", summary = "The families and plan names a listing can be narrowed to",
		description = "What `GET /api/plan`'s `family` and `plan` parameters accept, so that they can be "
			+ "offered rather than typed. Plan names include the ones the suite no longer publishes - a "
			+ "listing of old plans is mostly made of those, and they are exactly what somebody clearing "
			+ "a database out is looking for - each marked `retired`.\n\n"
			+ "Deliberately not `/api/plan/available`, which answers the same question but carries every "
			+ "plan's modules, variants and configuration fields with it: 680 KB against this one's few.\n\n"
			+ "Readable on a public request (`?public=true`) as well as by a logged in user: this is the "
			+ "plan registry, the same material `/api/plan/available` already answers with on one, and "
			+ "holds nothing about anybody's data. Pinned by `scripts/run-security-tests.py`.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<Object> getPlanFilterOptions() {
		return new ResponseEntity<>(filterOptions(), HttpStatus.OK);
	}

	/**
	 * @return the filter options, computed once. The registry cannot change while the process
	 *         runs, and building this walks every plan's variant summary - five stream
	 *         collections per plan over its modules and parameters - which is far too much to
	 *         repeat on every plans.html load, for every user, which is when it is asked for.
	 */
	private Map<String, Object> filterOptions() {

		Map<String, Object> cached = planFilterOptions;
		if (cached != null) {
			return cached;
		}

		List<String> families = new ArrayList<>();
		List<Map<String, Object>> plans = new ArrayList<>();

		for (String family : specFamilyResolver.familyOrder()) {
			// familyOrder ends with the two buckets a plan can fall into rather than families of
			// their own ("No plan", "Other / retired"); neither has any plan name to offer, so
			// both drop out here rather than being listed as a filter that matches nothing
			Set<String> names = specFamilyResolver.plansEverIn(family);
			if (names.isEmpty()) {
				continue;
			}
			families.add(family);
			names.stream().sorted().forEach(name -> plans.add(Map.of(
				"name", name,
				"family", family,
				"retired", !specFamilyResolver.isKnownPlan(name))));
		}

		// parameter -> the values it may take, per plan: 25 KB for all of them, which is worth
		// carrying here so that choosing a plan needs no second request. Only the names; the
		// registry's own summary also holds display names, descriptions and the configuration
		// fields each value shows or hides, which is 200 KB of what a filter has no use for.
		Map<String, Map<String, List<String>>> variants = new TreeMap<>();
		for (VariantService.TestPlanHolder holder : variantService.getTestPlans()) {
			Map<String, List<String>> values = variantValues(holder.getVariantSummary());
			if (!values.isEmpty()) {
				variants.put(holder.info.testPlanName(), values);
			}
		}

		Map<String, Object> options = Map.of("families", families, "plans", plans, "variants", variants);
		// benign race: two callers may both build it, and either answer is the same
		planFilterOptions = options;
		return options;
	}

	/**
	 * @param summary what {@code TestPlanHolder.getVariantSummary()} answers: each plan level
	 *                variant parameter mapped to its {@code variantInfo} and its
	 *                {@code variantValues}
	 * @return each of those parameters mapped to the value names alone, sorted; empty when the
	 *         summary is not the shape this expects, so a change there narrows the filter
	 *         controls rather than breaking the endpoint
	 */
	static Map<String, List<String>> variantValues(Object summary) {

		if (!(summary instanceof Map<?, ?> parameters)) {
			return Map.of();
		}
		Map<String, List<String>> values = new TreeMap<>();
		for (Map.Entry<?, ?> parameter : parameters.entrySet()) {
			if (parameter.getValue() instanceof Map<?, ?> details
				&& details.get("variantValues") instanceof Map<?, ?> allowed) {
				values.put(String.valueOf(parameter.getKey()),
					allowed.keySet().stream().map(String::valueOf).sorted().toList());
			}
		}
		return values;
	}

	// a literal path, like /plan/delete-status: no plan id can contain a hyphen
	@GetMapping(value = "/plan/delete-preview", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "previewBulkPlanDelete", summary = "How many plans a bulk delete would remove, without removing any (admin only)",
		description = "Takes the same parameters as `DELETE /api/plan` and answers what it would do: how "
			+ "many plans the listing shows, how many of those may be deleted, how many are kept because "
			+ "they are immutable or published, and the number to send back as `confirm`.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
		@ApiResponse(responseCode = "400", description = "A filter parameter could not be used"),
		@ApiResponse(responseCode = "403", description = "You must be an admin")
	})
	public ResponseEntity<Object> bulkDeletePreview(
		@Parameter(description = "Delete at most this many plans, oldest first")
		@RequestParam(required = false) Integer limit,
		@Parameter(hidden = true) HttpServletRequest request) {

		if (!authenticationFacade.isAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		PlanListFilter filter;
		PlanOwner owner;
		try {
			filter = PlanListFilter.parse(request.getParameterMap(), specFamilyResolver);
			owner = PlanOwner.parse(request.getParameterMap());
		} catch (IllegalArgumentException e) {
			return badRequest(e.getMessage());
		}

		String search = PaginationRequest.searchTerm(QueryParams.first(request.getParameterMap(), "search"));

		// the same refusals as the delete itself: a preview of a request that could never run is
		// worse than useless, and a negative limit would otherwise report a negative target
		ResponseEntity<Object> refusal = refuseUnusableBulkRequest(filter, owner, search, limit);
		if (refusal != null) {
			return refusal;
		}

		return new ResponseEntity<>(
			bulkPlanDeleter.preview(DBTestPlanService.ownerScope(null, owner), filter, search, limit),
			HttpStatus.OK);
	}

	/**
	 * The rules {@code DELETE /api/plan} and its preview both hold to.
	 *
	 * @param filter the narrowing the caller asked for
	 * @param owner  the account they asked to narrow to, or null
	 * @param search the quoted term they asked to search for, or null
	 * @param limit  the most plans to delete, or null
	 * @return the 400 to send, or null when the request can be acted on
	 */
	static ResponseEntity<Object> refuseUnusableBulkRequest(PlanListFilter filter, PlanOwner owner,
																	String search, Integer limit) {
		// a search narrows a listing as much as any filter does, and the page offers to move the
		// search box's term into the listing precisely so that what it finds can be deleted
		if (filter.isEmpty() && owner == null && search == null) {
			return badRequest("a bulk delete needs a filter: send at least one of owner, search, family, "
				+ "plan, cert, immutable, variant.<parameter>, from or to");
		}
		if (limit != null && limit <= 0) {
			return badRequest("limit must be a positive number of plans, not " + limit);
		}
		return null;
	}

	@DeleteMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "bulkDeleteTestPlans", summary = "Delete every plan a filtered listing shows, and their tests and logs (admin only)",
		description = "Takes exactly the parameters `GET /api/plan` takes and deletes what that listing "
			+ "would show, so the way to see what this will do is to list it first. Deleting millions of "
			+ "documents is far too long for a request, so the work runs in the background: this returns "
			+ "202 and `GET /api/plan/delete-status` reports progress.\n\n"
			+ "Two kinds of plan are never deleted, whatever is asked for: one a certification package has "
			+ "been downloaded for (`immutable`), and one that has been published, since a link to it may "
			+ "be in circulation. They are left out of the count as well as of the deleting.\n\n"
			+ "Refused unless at least one filter is present - there is no way to ask this to delete "
			+ "everything - and unless `confirm` is the number of plans it is about to delete, so that a "
			+ "listing that has changed since it was looked at stops the request rather than deleting "
			+ "something else.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "202", description = "Deleting has started; poll /api/plan/delete-status"),
		@ApiResponse(responseCode = "400", description = "No filter was sent, a parameter could not be used, "
			+ "or `confirm` is not what would be deleted"),
		@ApiResponse(responseCode = "403", description = "You must be an admin to delete plans in bulk"),
		@ApiResponse(responseCode = "409", description = "A bulk delete is already running")
	})
	public ResponseEntity<Object> deletePlans(
		@Parameter(description = "Delete at most this many plans, oldest first. Omit to delete all of them.")
		@RequestParam(required = false) Integer limit,
		@Parameter(description = "The number of plans this will delete, as a check that it is what was seen")
		@RequestParam(required = false) Long confirm,
		@Parameter(hidden = true) HttpServletRequest request) {

		if (!authenticationFacade.isAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		PlanListFilter filter;
		PlanOwner owner;
		try {
			filter = PlanListFilter.parse(request.getParameterMap(), specFamilyResolver);
			owner = PlanOwner.parse(request.getParameterMap());
		} catch (IllegalArgumentException e) {
			return badRequest(e.getMessage());
		}

		// quoted by the same helper the listing uses, so a delete removes what was listed
		String search = PaginationRequest.searchTerm(QueryParams.first(request.getParameterMap(), "search"));

		ResponseEntity<Object> refusal = refuseUnusableBulkRequest(filter, owner, search, limit);
		if (refusal != null) {
			return refusal;
		}

		// null principal: only an admin gets this far, and an admin may see every plan
		Criteria scope = DBTestPlanService.ownerScope(null, owner);
		// target() rather than preview(): the delete needs one number, and the count it leaves
		// out is a second pass over every plan the filter matches
		long target = bulkPlanDeleter.target(scope, filter, search, limit);

		if (confirm == null || confirm.longValue() != target) {
			return badRequest(("confirm must be the number of plans this would delete, which is %d"
				+ " (immutable and published plans are already left out of that)").formatted(target));
		}

		try {
			return new ResponseEntity<>(bulkPlanDeleter.start(scope, filter, search, target), HttpStatus.ACCEPTED);
		} catch (IllegalStateException e) {
			return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.CONFLICT);
		}
	}

	// a literal path rather than a plan id: ids are alphanumeric, so no plan can ever be called
	// "delete-status", and Spring matches the literal ahead of the /plan/{id} template anyway
	@GetMapping(value = "/plan/delete-status", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getBulkPlanDeleteStatus", summary = "How far the running (or last) bulk plan delete has got (admin only)")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
		@ApiResponse(responseCode = "403", description = "You must be an admin")
	})
	public ResponseEntity<Object> bulkDeleteStatus() {

		if (!authenticationFacade.isAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		return new ResponseEntity<>(bulkPlanDeleter.progress(), HttpStatus.OK);
	}

	@PostMapping(value = "/plan/delete-cancel", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "cancelBulkPlanDelete", summary = "Stop the running bulk plan delete (admin only)",
		description = "The job stops at the end of the batch it is in; what it has already deleted stays "
			+ "deleted. Re-running the same request carries on from where it stopped.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Asked it to stop"),
		@ApiResponse(responseCode = "403", description = "You must be an admin")
	})
	public ResponseEntity<Object> bulkDeleteCancel() {

		if (!authenticationFacade.isAdmin()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		bulkPlanDeleter.cancel();
		return new ResponseEntity<>(bulkPlanDeleter.progress(), HttpStatus.OK);
	}

	private static ResponseEntity<Object> badRequest(String message) {
		return new ResponseEntity<>(Map.of("error", message), HttpStatus.BAD_REQUEST);
	}

	@DeleteMapping(value = "/plan/{id}")
	@Operation(operationId = "deleteTestPlan", summary = "Delete a test plan and related configuration. Requires the plan to be mutable.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Deleted successfully", content = @Content),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions to delete test plan", content = @Content),
		@ApiResponse(responseCode = "404", description = "Could not find a plan with the given id, belonging to the user", content = @Content),
		@ApiResponse(responseCode = "405", description = "The plan is immutable and cannot be deleted", content = @Content)
	})
	public ResponseEntity<Void> deleteMutableTestPlan(
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

		// Stop any still-running module first, synchronously: stop() writes its final log
		// entries, which the deleteTests() below then removes. Stopping in the background (as
		// cancelTest does), or after deleteTests, would let a running module keep writing
		// EVENT_LOG rows past the delete and orphan them. Shared with the bulk delete, which
		// has to do exactly this for the same reason.
		bulkPlanDeleter.stopAnyRunning(testIds);

		// Known race, deliberately not fixed. testIds is the snapshot taken above, and
		// createTest() saves TEST_INFO before attaching the instance to the plan (see
		// DBTestInfoService.createTest), so a concurrent create can either attach after the
		// snapshot - leaving a test that outlives its plan - or attach before it and still write
		// its "Test instance created" entry (TestRunner.createTest) after we have deleted,
		// leaving EVENT_LOG rows with no TEST_INFO row. Reordering the deletes does not fix the
		// second case: that needs the create serialised against this method, or the plan attach
		// made the create's last write. Both are out of proportion to the only way to reach this
		// - a plan's owner deleting it while simultaneously creating a test in it - and the
		// residue is inert: the module cannot resurrect, since stop() leaves it INTERRUPTED and
		// both setStatusInternal() and call() refuse to act from a terminal state.
		infoService.deleteTests(testIds);
		planService.deleteMutableTestPlan(id);

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

}
