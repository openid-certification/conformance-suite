package net.openid.conformance.info;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import net.openid.conformance.logging.DBEventLog;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.TestModule.Result;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.VariantSelection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class DBTestInfoService implements TestInfoService {

	public static final String COLLECTION = "TEST_INFO";

	private static final Logger logger = LoggerFactory.getLogger(DBTestInfoService.class);

	@Value("${fintechlabs.version}")
	private String version;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private TestInfoRepository testInfos;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private TestPlanService testPlanService;

	@SuppressWarnings("CacheLoaderNull")
	//Private cache for holding test owners without having to hit the db
	LoadingCache<String, ImmutableMap<String, String>> testOwnerCache = CacheBuilder.newBuilder()
		.maximumSize(1000)
		.expireAfterAccess(30, TimeUnit.MINUTES) // is 30 minutes a good time out? too much? too little?
		.build(
			new CacheLoader<String, ImmutableMap<String, String>>() {
				@Override
				public ImmutableMap<String, String> load(String key) {
					Query query = Query.query(Criteria.where("_id").is(key));
					Document test = mongoTemplate.findOne(query, Document.class, COLLECTION);
					if (test != null &&
						test.containsKey("owner")) {
						Document owner = test.get("owner", Document.class);
						String iss = owner.getString("iss");
						String sub = owner.getString("sub");
						return ImmutableMap.of("sub", sub, "iss", iss);
					}
					return null;
				}
			});

	@Override
	public void createTest(String id, String testName, VariantSelection variant, VariantSelection variantFromPlanDefinition, String url, JsonObject config, String alias, Instant started, String planId, String description, String summary, String publish) {
		ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();

		testInfos.save(new TestInfo(
				id,
				testName,
				variant,
				started,
				config,
				description, // for this instance,
				alias,
				owner,
				planId,
				version,
				summary, // from the test definition,
				publish));

		if (planId != null) {
			testPlanService.updateTestPlanWithModule(planId, testName, variantFromPlanDefinition, id);
		}
	}

	@Override
	public void updateTestResult(String id, Result result) {

		Criteria criteria = new Criteria();
		criteria.and("_id").is(id);
		if (Result.REVIEW.equals(result)) {
			criteria.and("result").ne("FAILED");
		}

		Query query = new Query(criteria);

		Update update = new Update();
		update.set("result", result);

		mongoTemplate.updateFirst(query, update, COLLECTION);

	}

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
		Document test = mongoTemplate.findOne(query, Document.class, COLLECTION);
		if (test != null &&
				test.containsKey("owner")) {
			Document owner = test.get("owner", Document.class);
			String iss = owner.getString("iss");
			String sub = owner.getString("sub");
			return ImmutableMap.of("sub", sub, "iss", iss);
		} else {
			return null;
		}
		*/
	}

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

		UpdateResult result = mongoTemplate.updateFirst(query, update, COLLECTION);

		return result.getMatchedCount() > 0;
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
		sortedMap.put("testName", "text");
		sortedMap.put("description", "text");

		collection.createIndex(new Document(sortedMap));
	}

	@Override
	public boolean deleteTests(List<String> ids) {

		Criteria criteria = Criteria.where("testId").in(ids);

		if (!authenticationFacade.isAdmin()) {
			criteria.and("owner").is(authenticationFacade.getPrincipal());
		}

		Query query = new Query(criteria);
		DeleteResult testInfoDeleteResult = mongoTemplate.remove(query, COLLECTION);
		DeleteResult logDeleteResult = mongoTemplate.remove(query, DBEventLog.COLLECTION);

		return testInfoDeleteResult.wasAcknowledged() && logDeleteResult.wasAcknowledged();
	}
}
