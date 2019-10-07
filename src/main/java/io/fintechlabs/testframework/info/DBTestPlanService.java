package io.fintechlabs.testframework.info;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;

import io.fintechlabs.testframework.CollapsingGsonHttpMessageConverter;
import io.fintechlabs.testframework.pagination.PaginationRequest;
import io.fintechlabs.testframework.pagination.PaginationResponse;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.variant.VariantSelection;

@Service
public class DBTestPlanService implements TestPlanService {

	public static final String COLLECTION = "TEST_PLAN";

	private static Logger logger = LoggerFactory.getLogger(DBTestInfoService.class);

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
	 * @param id
	 */
	@Override
	public void updateTestPlanWithModule(String planId, String testName, String id) {

		Criteria criteria = new Criteria();
		criteria.and("_id").is(planId);
		criteria.and("modules.testModule").is(testName);

		Query query = new Query(criteria);

		Update update = new Update();
		update.push("modules.$.instances", id);

		mongoTemplate.updateFirst(query, update, COLLECTION);


	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#createTestPlan(java.lang.String, java.lang.String, com.google.gson.JsonObject, java.util.Map, io.fintechlabs.testframework.plan.TestPlan)
	 */
	@Override
	public void createTestPlan(String id, String planName, VariantSelection variant, JsonObject config, String description, String[] testModules, String summary, String publish) {

		ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();

		plans.save(new Plan(
				id,
				planName,
				variant,
				config,
				Instant.now(),
				owner,
				description, // for the specific instance
				testModules,
				version,
				summary, // from the plan definition
				publish));
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getTestPlan(java.lang.String)
	 */
	@Override
	public Plan getTestPlan(String id) {

		if (!authenticationFacade.isAdmin()) {
			return plans.findByIdAndOwner(id, authenticationFacade.getPrincipal()).orElse(null);
		} else {
			return plans.findById(id).orElse(null);
		}
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getPublicPlan(java.lang.String)
	 */
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

		JsonObject config = new JsonParser().parse(json).getAsJsonObject();

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

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getPaginatedPlansForCurrentUser()
	 */
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

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getPaginatedPublicPlans()
	 */
	@Override
	public PaginationResponse<PublicPlan> getPaginatedPublicPlans(PaginationRequest page) {

		return page.getResponse(
				p -> plans.findAllPublic(p),
				(s, p) -> plans.findAllPublicSearch(s, p));
	}

	/*
	 * (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#publishTestPlan(java.lang.String, java.lang.String)
	 */
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

		if (result.getMatchedCount() == 0)
			return false;

		// We need to update all the latest test results (if possible) as well

		// The goal of the mess below is to get the last value in each of the
		// "instances" arrays for the modules in this plan.

		Object testModules = mongoTemplate.getCollection(COLLECTION)
				.find(new Document("_id", id))
				.first()
				.get("modules");

		Object[] latestTestIds = ((List<?>) testModules)
				.stream()
				.map(mod -> (List<?>) ((Map) mod).get("instances"))
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
	public VariantSelection getTestPlanVariant(String planId) {
		Plan testPlan = getTestPlan(planId);

		if (testPlan != null) {

			return testPlan.getVariant();
		}

		return null;
	}

	@Override
	public void createIndexes(){
		MongoCollection<Document> collection = mongoTemplate.getCollection(COLLECTION);
		collection.createIndex(new Document("$**", "text"));
	}
}
