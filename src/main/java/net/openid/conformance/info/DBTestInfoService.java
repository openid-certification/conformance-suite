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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

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
		.expireAfterAccess(Duration.ofMinutes(30)) // is 30 minutes a good time out? too much? too little?
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

		// Almost every read here is scoped to the calling user via an exact match on
		// the whole owner sub-document, and migrateOwnership updates by it. Without
		// this, all of those are collection scans. Creating an index that already
		// exists is a no-op, so this is safe to run on every startup.
		collection.createIndex(new Document("owner", 1));
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

	@Override
	public MigrationCounts migrateOwnership(String oldIss, String oldSub) {

		ImmutableMap<String, String> newOwner = authenticationFacade.getPrincipal();
		ImmutableMap<String, String> owner = ImmutableMap.of(
			"sub", oldSub,
			"iss", oldIss
		);

		// A null new owner would rewrite every matching document's owner to null,
		// orphaning the tests it was supposed to hand over.
		if (newOwner == null || newOwner.equals(owner)) {
			return MigrationCounts.NONE;
		}

		Query query = new Query(Criteria.where("owner").is(owner));

		Update udt = Update.update("owner", newOwner);

		long tests = mongoTemplate.updateMulti(query, udt, COLLECTION).getModifiedCount();

		// Log entries carry their own copy of the owner, under "testOwner", and are
		// access-controlled on it independently of TEST_INFO (LogApi filters on
		// testOwner; DBImageService does too, because uploaded screenshots are
		// stored as EVENT_LOG documents). Migrating TEST_INFO alone leaves the user
		// seeing their tests in the listings but getting an empty log and no
		// screenshots when they open one.
		Query logQuery = new Query(Criteria.where("testOwner").is(owner));
		Update logUpdate = Update.update("testOwner", newOwner);

		long logEntries = mongoTemplate.updateMulti(logQuery, logUpdate, DBEventLog.COLLECTION).getModifiedCount();

		MigrationCounts counts = new MigrationCounts(tests, logEntries);
		if (!counts.movedNothing()) {
			// getTestOwner answers from this cache for up to 30 minutes, so without
			// this the owner checks in ImageApi keep comparing against the identity
			// the migration just replaced, and keep failing. Invalidating everything
			// rather than the migrated ids: the cache holds at most 1000 entries and
			// reloads on demand, so finding out exactly which ones moved would cost
			// more than rebuilding them.
			testOwnerCache.invalidateAll();

			logger.info("Migrated ownership of legacy records. legacyIss={} legacySub={} newOwner={} tests={} logEntries={}",
				oldIss, oldSub, newOwner.get("sub"), tests, logEntries);
		} else {
			logger.debug("No legacy records to migrate. legacyIss={} legacySub={}", oldIss, oldSub);
		}

		return counts;
	}

}
