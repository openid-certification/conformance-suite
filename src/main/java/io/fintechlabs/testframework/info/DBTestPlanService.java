package io.fintechlabs.testframework.info;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Field;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;

import io.fintechlabs.testframework.CollapsingGsonHttpMessageConverter;
import io.fintechlabs.testframework.pagination.PaginationRequest;
import io.fintechlabs.testframework.security.AuthenticationFacade;

@Service
public class DBTestPlanService implements TestPlanService {

	public static final String COLLECTION = "TEST_PLAN";

	private static Logger logger = LoggerFactory.getLogger(DBTestInfoService.class);

	@Value("${fintechlabs.version}")
	private String version;

	@Autowired
	private MongoTemplate mongoTemplate;

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
	public void createTestPlan(String id, String planName, JsonObject config, String description, String[] testModules, String summary, String publish) {

		ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();

		BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
			.add("_id", id)
			.add("planName", planName)
			.add("config", config)
			.add("started", Instant.now().toString())
			.add("owner", owner)
			.add("description", description) // for the specific instance
			.add("version", version)
			.add("summary", summary) // from the plan definition
			.add("publish", publish);

		List<DBObject> moduleStructure = new ArrayList<>();

		for (String module : testModules) {
			BasicDBObjectBuilder moduleBuilder = BasicDBObjectBuilder.start()
				.add("testModule", module)
				.add("instances", Collections.emptyList());

			moduleStructure.add(moduleBuilder.get());
		}

		documentBuilder.add("modules", moduleStructure);


		mongoTemplate.insert(documentBuilder.get(), COLLECTION);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getTestPlan(java.lang.String)
	 */
	@Override
	public Map getTestPlan(String id) {

		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);

		if (!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}

		Query query = new Query(criteria);

		return mongoTemplate.getCollection(COLLECTION).find(query.getQueryObject()).first();
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getPublicPlan(java.lang.String)
	 */
	@Override
	public Map getPublicPlan(String id) {

		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);
		criteria.and("publish").in("summary", "everything");

		Query query = new Query(criteria);

		query.fields()
				.include("_id")
				.include("planName")
				.include("description")
				.include("started")
				.include("modules")
				.include("publish")
				.include("version");

		return mongoTemplate
				.getCollection(COLLECTION)
				.find(query.getQueryObject())
				.projection(query.getFieldsObject())
				.first();
	}

	@Override
	public JsonObject getModuleConfig(String planId, String moduleName) {
		Map testPlan = getTestPlan(planId);

		List modules = (List) testPlan.get("modules");

		boolean found = false;

		for (Object o : modules)
		{
			Map module = (Map) o;
			if (module.containsValue(moduleName)) {
				found = true;
			}
		}

		if (!found) {
			// the user has asked to create a module that isn't part of the plan
			return null;
		}

		Object dbConfig = testPlan.get("config");

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
	public Map getPaginatedPlansForCurrentUser(PaginationRequest page) {

		Criteria criteria = new Criteria();

		if (!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}

		return page.getResults(mongoTemplate.getCollection(COLLECTION), criteria);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getPaginatedPublicPlans()
	 */
	@Override
	public Map getPaginatedPublicPlans(PaginationRequest page) {

		Criteria criteria = new Criteria("publish").in("summary", "everything");

		Field fields = new Field()
				.include("_id")
				.include("planName")
				.include("description")
				.include("started")
				.include("modules")
				.include("publish");

		return page.getResults(mongoTemplate.getCollection(COLLECTION), criteria, fields);
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
	public void createIndexes(){
		MongoCollection<Document> collection = mongoTemplate.getCollection(COLLECTION);
		collection.createIndex(new Document("planName", 1));
		collection.createIndex(new Document("description", 1));
		collection.createIndex(new Document("started", 1));
		collection.createIndex(new Document("owner", 1));
		collection.createIndex(new Document("publish", 1));
		collection.createIndex(new Document("$**", "text"));
	}
}
