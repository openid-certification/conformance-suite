package io.fintechlabs.testframework.info;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.Cursor;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

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

		return getSinglePlan(id, false);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getPublicPlan(java.lang.String)
	 */
	@Override
	public Map getPublicPlan(String id) {

		return getSinglePlan(id, true);
	}

	private Map getSinglePlan(String id, boolean publishedOnly) {

		boolean isAdmin = authenticationFacade.isAdmin();

		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);
		if (publishedOnly) {
			criteria.and("publish").in("summary", "everything");
		} else if (!isAdmin) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}

		List<DBObject> pipeline = new ArrayList<DBObject>();
		pipeline.add(new BasicDBObject("$match", criteria.getCriteriaObject()));
		pipeline.addAll(getTestPlanProjection(publishedOnly, isAdmin));

		// Force the driver to include a 'cursor' option - Mongo complains otherwise.
		AggregationOptions options = AggregationOptions.builder()
				.outputMode(AggregationOptions.OutputMode.CURSOR)
				.build();

		Cursor results = mongoTemplate.getCollection(COLLECTION).aggregate(pipeline, options);

		if (results.hasNext())
			return results.next().toMap();
		else
			return null;
	}

	@Override
	public JsonObject getModuleConfig(String planId, String moduleName) {
		Map testPlan = getTestPlan(planId);

		BasicDBList modules = (BasicDBList) testPlan.get("modules");

		boolean found = false;

		for (Object o : modules)
		{
			BasicDBObject module = (BasicDBObject) o;
			if (module.containsValue(moduleName)) {
				found = true;
			}
		}

		if (!found) {
			// the user has asked to create a module that isn't part of the plan
			return null;
		}

		DBObject dbConfig = (DBObject) testPlan.get("config");

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

		return getPaginatedPlans(page, false);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestPlanService#getPaginatedPublicPlans()
	 */
	@Override
	public Map getPaginatedPublicPlans(PaginationRequest page) {

		return getPaginatedPlans(page, true);
	}

	private Map getPaginatedPlans(PaginationRequest page, boolean publishedOnly) {

		boolean isAdmin = authenticationFacade.isAdmin();

		// Limit result set
		Criteria criteria = new Criteria();

		if (publishedOnly) {
			criteria.and("publish").in("summary", "everything");
		} else if (!isAdmin) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}

		List<DBObject> projection = getTestPlanProjection(publishedOnly, isAdmin);

		return page.getResults(mongoTemplate.getCollection(COLLECTION), criteria.getCriteriaObject(), projection);
	}

	private List<DBObject> getTestPlanProjection(boolean publishedOnly, boolean isAdmin) {

		List<DBObject> projection = new ArrayList<DBObject>();

		// Filter test modules to return only user-owned instances
		if (publishedOnly || !isAdmin) {
			List<DBObject> testCriteria = new ArrayList<DBObject>();
			testCriteria.add(new BasicDBObject("$in", new String[] { "$_id", "$$instances" }));
			if (publishedOnly)
				testCriteria.add(new BasicDBObject("$in", new Object[] { "$publish", new String[] { "summary", "everything" } }));
			else if (!isAdmin)
				testCriteria.add(new BasicDBObject("$eq", new Object[] { "$owner", authenticationFacade.getPrincipal() }));

			projection.add(new BasicDBObject("$unwind",
					BasicDBObjectBuilder.start()
							.add("path", "$modules")
							.add("preserveNullAndEmptyArrays", true)
							.get()));
			projection.add(new BasicDBObject("$lookup",
					BasicDBObjectBuilder.start()
							.add("from", DBTestInfoService.COLLECTION)
							.add("let", new BasicDBObject("instances", "$modules.instances"))
							.add("pipeline", new DBObject[] {
									new BasicDBObject("$match",
											new BasicDBObject("$expr", new BasicDBObject("$and", testCriteria))),
									new BasicDBObject("$addFields",
											new BasicDBObject("sort",
													new BasicDBObject("$indexOfArray", new String[] { "$$instances", "$_id" }))),
									new BasicDBObject("$sort", new BasicDBObject("sort", 1))
							})
							.add("as", "modules.instances")
							.get()));
			projection.add(new BasicDBObject("$addFields",
					new BasicDBObject("modules.instances", "$modules.instances._id")));
			projection.add(new BasicDBObject("$group",
					BasicDBObjectBuilder.start()
							.add("_id", "$_id")
							.add("planName", new BasicDBObject("$first", "$planName"))
							.add("config", new BasicDBObject("$first", "$config"))
							.add("started", new BasicDBObject("$first", "$started"))
							.add("owner", new BasicDBObject("$first", "$owner"))
							.add("description", new BasicDBObject("$first", "$description"))
							.add("modules", new BasicDBObject("$push", "$modules"))
							.add("publish", new BasicDBObject("$first", "$publish"))
							.get()));
		}

		if (publishedOnly) {
			projection.add(new BasicDBObject("$project",
					BasicDBObjectBuilder.start()
							.add("_id", 1)
							.add("planName", 1)
							.add("description", 1)
							.add("started", 1)
							.add("modules", 1)
							.add("publish", 1)
							.get()));
		}

		return projection;
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

		WriteResult result = mongoTemplate.updateFirst(query, update, COLLECTION);

		if (!result.isUpdateOfExisting())
			return false;

		// We need to update all the latest test results (if possible) as well

		// The goal of the mess below is to get the last value in each of the
		// "instances" arrays for the modules in this plan.

		Object testModules = mongoTemplate.getCollection(COLLECTION)
				.findOne(BasicDBObjectBuilder.start()
								.add("_id", id)
								.get())
				.get("modules");

		Object[] latestTestIds = ((BasicDBList) testModules)
				.stream()
				.map(mod -> (BasicDBList) ((BasicDBObject) mod).get("instances"))
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
		DBCollection collection = mongoTemplate.getCollection(COLLECTION);
		collection.createIndex(new BasicDBObject("planName", 1));
		collection.createIndex(new BasicDBObject("description", 1));
		collection.createIndex(new BasicDBObject("started", 1));
		collection.createIndex(new BasicDBObject("owner", 1));
		collection.createIndex(new BasicDBObject("publish", 1));
		collection.createIndex(BasicDBObjectBuilder.start("$**", "text").get());
	}
}
