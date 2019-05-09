package io.fintechlabs.testframework.info;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;

import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.testmodule.TestModule.Result;
import io.fintechlabs.testframework.testmodule.TestModule.Status;

@Service
public class DBTestInfoService implements TestInfoService {

	public static final String COLLECTION = "TEST_INFO";

	private static Logger logger = LoggerFactory.getLogger(DBTestInfoService.class);

	@Value("${fintechlabs.version}")
	private String version;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private TestPlanService testPlanService;

	//Private cache for holding test owners without having to hit the db
	LoadingCache<String, ImmutableMap<String, String>> testOwnerCache = CacheBuilder.newBuilder()
		.maximumSize(1000)
		.expireAfterAccess(30, TimeUnit.MINUTES) // is 30 minutes a good time out? too much? too little?
		.build(
			new CacheLoader<String, ImmutableMap<String, String>>() {
				@Override
				public ImmutableMap<String, String> load(String key) {
					Query query = Query.query(Criteria.where("_id").is(key));
					BasicDBObject test = mongoTemplate.findOne(query, BasicDBObject.class, COLLECTION);
					if (test != null &&
						test.containsField("owner")) {
						BasicDBObject owner = (BasicDBObject) test.get("owner");
						String iss = owner.getString("iss");
						String sub = owner.getString("sub");
						return ImmutableMap.of("sub", sub, "iss", iss);
					}
					return null;
				}
			});

	@Override
	public void createTest(String id, String testName, String url, JsonObject config, String alias, Instant started, String planId, String description, String summary, String publish) {
		ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();

		BasicDBObjectBuilder documentBuilder = BasicDBObjectBuilder.start()
			.add("_id", id)
			.add("testId", id)
			.add("testName", testName)
			.add("started", started.toString())
			.add("config", config)
			.add("description", description) // for this instance
			.add("alias", alias)
			.add("owner", owner)
			.add("planId", planId)
			.add("status", Status.CREATED)
			.add("version", version)
			.add("summary", summary) // from the test definition
			.add("publish", publish);

		mongoTemplate.insert(documentBuilder.get(), COLLECTION);

		if (planId != null) {
			testPlanService.updateTestPlanWithModule(planId, testName, id);
		}
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestInfoService#updateTestResult(java.lang.String, io.fintechlabs.testframework.testmodule.TestModule.Result)
	 */
	@Override
	public void updateTestResult(String id, Result result) {

		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);

		Query query = new Query(criteria);

		Update update = new Update();
		update.set("result", result);

		mongoTemplate.updateFirst(query, update, COLLECTION);

	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestInfoService#updateTestStatus(java.lang.String, io.fintechlabs.testframework.testmodule.TestModule.Status)
	 */
	@Override
	public void updateTestStatus(String id, Status status) {

		// find the existing entity
		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);

		Query query = new Query(criteria);

		Update update = new Update();
		update.set("status", status);

		mongoTemplate.updateFirst(query, update, COLLECTION);

	}

	@Override
	public ImmutableMap<String, String> getTestOwner(String testId) {
		try {
			return testOwnerCache.get(testId);
		} catch (ExecutionException e) {
			logger.error("ExecutionException while looking up owner for testId: " + testId, e);
		}
		return null;

		/* Non caching code here
		Query query = Query.query(Criteria.where("_id").is(id));
		BasicDBObject test = mongoTemplate.findOne(query, BasicDBObject.class, COLLECTION);
		if (test != null &&
				test.containsField("owner")) {
			BasicDBObject owner = (BasicDBObject)test.get("owner");
			String iss = owner.getString("iss");
			String sub = owner.getString("sub");
			return ImmutableMap.of("sub", sub, "iss", iss);
		} else {
			return null;
		}
		*/
	}

	/*
	 * (non-Javadoc)
	 * @see io.fintechlabs.testframework.info.TestInfoService#publishTest(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean publishTest(String id, String publish) {

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

		return result.isUpdateOfExisting();
	}

	@Override
	public void createIndexes(){
		DBCollection collection = mongoTemplate.getCollection(COLLECTION);
		collection.createIndex(BasicDBObjectBuilder.start("$**", "text").get());
	}
}
