package net.openid.conformance.info;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import net.openid.conformance.CollapsingGsonHttpMessageConverter;
import net.openid.conformance.pagination.PaginationRequest;
import net.openid.conformance.pagination.PaginationResponse;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.variant.VariantSelection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

@Service
public class DBTestPlanService implements TestPlanService {

	public static final String COLLECTION = "TEST_PLAN";

	@Value("${fintechlabs.version}")
	private String version;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private PlanRepository plans;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	private Gson gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson();

	/**
	 * @param planId
	 * @param testName
	 * @param variant
	 * @param id
	 */
	@Override
	public void updateTestPlanWithModule(String planId, String testName, VariantSelection variant, String id) {

		Criteria criteria = new Criteria();
		criteria.and("_id").is(planId);

		Query query = new Query(criteria);

		Update update = new Update();
		update.push("modules.$[module].instances", id);
		Criteria updateCriteria = new Criteria();
		if (variant != null) {
			variant.getVariant().forEach((name, value) -> {
				updateCriteria.and("module.variant."+name).is(value);
			});
		}
		updateCriteria.and("module.testModule").is(testName);
		update.filterArray(updateCriteria);

		var result = mongoTemplate.updateFirst(query, update, COLLECTION);
		if (result.getModifiedCount() != 1) {
			throw new RuntimeException("failed to add module '%s'('%s') to test plan id '%s' - modifiedCount=%d".formatted(
				testName, variant != null ? variant.toString() : "variant=null", planId, result.getModifiedCount()));
		}
	}

	@Override
	public void createTestPlan(String id, String planName, VariantSelection variant, JsonObject config, String description, String certificationProfileName, List<Plan.Module> testModules, String summary, String publish) {

		ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();

		plans.save(new Plan(
				id,
				planName,
				variant,
				config,
				Instant.now(),
				owner,
				description, // for the specific instance
				certificationProfileName,
				testModules,
				version,
				summary, // from the plan definition
				publish));
	}

	@Override
	public Plan getTestPlan(String id) {

		if (!authenticationFacade.isAdmin()) {
			return plans.findByIdAndOwner(id, authenticationFacade.getPrincipal()).orElse(null);
		} else {
			return plans.findById(id).orElse(null);
		}
	}

	@Override
	public PublicPlan getPublicPlan(String id) {

		return plans.findByIdPublic(id).orElse(null);
	}

	@Override
	public JsonObject getModuleConfig(String planId, String moduleName) {
		Plan testPlan = getTestPlan(planId);

		List<Plan.Module> modules = testPlan.getModules();

		boolean found = false;

		for (Plan.Module module : modules)
		{
			if (module.getTestModule().equals(moduleName)) {
				found = true;
			}
		}

		if (!found) {
			// the user has asked to create a module that isn't part of the plan
			return null;
		}

		Document dbConfig = testPlan.getConfig();

		String json = gson.toJson(dbConfig);

		JsonObject config = JsonParser.parseString(json).getAsJsonObject();

		if (config.has("override")) {
			JsonObject override = config.getAsJsonObject("override");
			config.remove("override");
			if (override.has(moduleName)) {
				// Move all the overridden elements up into the configuration
				JsonObject overrides = override.getAsJsonObject(moduleName);
				for (Map.Entry<String, JsonElement> entry : overrides.entrySet()) {
					config.add(entry.getKey(), entry.getValue());
				}
			}
		}

		return config;
	}

	@Override
	public PaginationResponse<Plan> getPaginatedPlansForCurrentUser(PaginationRequest page) {

		if (!authenticationFacade.isAdmin()) {
			Map<String, String> owner = authenticationFacade.getPrincipal();
			return page.getResponse(
					p -> plans.findAllByOwner(owner, p),
					(s, p) -> plans.findAllByOwnerSearch(owner, s, p));
		} else {
			return page.getResponse(
					p -> plans.findAll(p),
					(s, p) -> plans.findAllSearch(s, p));
		}
	}

	@Override
	public PaginationResponse<PublicPlan> getPaginatedPublicPlans(PaginationRequest page) {

		return page.getResponse(
				p -> plans.findAllPublic(p),
				(s, p) -> plans.findAllPublicSearch(s, p));
	}

	@Override
	public boolean publishTestPlan(String id, String publish) {

		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);

		if (!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}

		if (publish == null) {
			if (!authenticationFacade.isAdmin()) {
				// Only admins may un-publish
				criteria.and("publish").is(null);
			}
		} else if (publish.equals("summary")) {
			if (!authenticationFacade.isAdmin()) {
				// Non-admins may only increase publish-level
				criteria.and("publish").in(null, "summary");
			}
		} else if (publish.equals("everything")) {
			// OK
		} else {
			// Invalid publish value
			return false;
		}

		Query query = new Query(criteria);
		Update update = new Update();
		update.set("publish", publish);

		UpdateResult result = mongoTemplate.updateFirst(query, update, COLLECTION);

		if (result.getMatchedCount() == 0) {
			return false;
		}

		// We need to update all the latest test results (if possible) as well

		// The goal of the mess below is to get the last value in each of the
		// "instances" arrays for the modules in this plan.

		Object testModules = mongoTemplate.getCollection(COLLECTION)
				.find(new Document("_id", id))
				.first()
				.get("modules");

		Object[] latestTestIds = ((List<?>) testModules)
				.stream()
				.map(mod -> (List<?>) ((Map<?,?>) mod).get("instances"))
				.filter(x -> !x.isEmpty())
				.map(x -> x.get(x.size() - 1))
				.toArray();

		// And now we plug the values back into a separate query in true
		// no-SQL fashion.

		criteria = new Criteria();
		criteria.and("_id").in(latestTestIds);
		criteria.and("planId").is(id);

		if (!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}

		query = new Query(criteria);

		// We can use the same update object
		mongoTemplate.updateMulti(query, update, DBTestInfoService.COLLECTION);

		return true;
	}

	@Override
	public boolean changeTestPlanImmutableStatus(String id, Boolean immutable) {

		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);

		if (!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}

		if (immutable == null || !immutable) {
			if (!authenticationFacade.isAdmin()) {
				// Only admins may make it mutable again
				criteria.and("immutable").ne(Boolean.TRUE);
			}
		}

		Query query = new Query(criteria);
		Update update = new Update();
		update.set("immutable", immutable);

		UpdateResult result = mongoTemplate.updateFirst(query, update, COLLECTION);

		if (result.getMatchedCount() == 0) {
			return false;
		}
		return true;
	}

	@Override
	public VariantSelection getTestPlanVariant(String planId) {
		Plan testPlan = getTestPlan(planId);

		if (testPlan != null) {

			return testPlan.getVariant();
		}

		return null;
	}

	@Override
	public void createIndexes(){
		// Drop any existing wildcard text index.
		//
		// This is required for the migration to a more targeted compound index.
		for (IndexInfo index: mongoTemplate.indexOps(COLLECTION).getIndexInfo()) {
			if (index.getName().equals("$**_text")) {
				mongoTemplate.indexOps(COLLECTION).dropIndex(index.getName());
				break;
			}
		}

		MongoCollection<Document> collection = mongoTemplate.getCollection(COLLECTION);

		SortedMap<String, Object> sortedMap = new TreeMap<>();
		sortedMap.put("planName", "text");
		sortedMap.put("description", "text");
		sortedMap.put("certificationProfileName", "text");

		collection.createIndex(new Document(sortedMap));
	}

	@Override
	public void deleteMutableTestPlan(String id) {
		Optional<Plan> maybePlan;
		if (!authenticationFacade.isAdmin()) {
			maybePlan = plans.findByIdAndOwner(id, authenticationFacade.getPrincipal());
		} else {
			maybePlan = plans.findById(id);
		}

		if(maybePlan.isEmpty()) {
			return;
		}

		Plan plan = maybePlan.get();
		if(plan.getImmutable() != null && plan.getImmutable()) {
			return;
		}

		plans.deleteById(id);
	}
}
