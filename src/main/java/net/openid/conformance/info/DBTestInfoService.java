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
import net.openid.conformance.pagination.PaginationRequest;
import net.openid.conformance.pagination.PaginationResponse;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.TestModule.Result;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.VariantSelection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
	public PaginationResponse<TestInfo> getPaginatedTestsForCurrentUser(PaginationRequest page, TestListFilter filter) {

		Criteria scope = authenticationFacade.isAdmin() ? null
				: Criteria.where("owner").is(authenticationFacade.getPrincipal());

		PaginationResponse<TestInfo> response = page.getSliceResponse((search, pageable) ->
				findSlice(scope, filter, search, pageable, TestInfo.class));
		attachPlanNames(response.data, null);
		return response;
	}

	@Override
	public PaginationResponse<PublicTestInfo> getPaginatedPublicTests(PaginationRequest page, TestListFilter filter) {

		PaginationResponse<PublicTestInfo> response = page.getSliceResponse((search, pageable) ->
				findSlice(DBTestPlanService.published(), filter, search, pageable, PublicTestInfo.class));
		// a published test belongs to a published plan, so this only ever narrows to what a
		// public reader could open anyway - it is here so that this listing can never be the
		// one place that says anything about an unpublished plan
		attachPlanNames(response.data, DBTestPlanService.published());
		return response;
	}

	/**
	 * Runs a listing. Results are read through {@code TestInfo} as {@code type}, so a public
	 * listing asks for {@link PublicTestInfo} and is projected in the database to the fields it
	 * may show.
	 *
	 * @param scope    the criteria that decide what the caller may see at all, or null for an
	 *                 admin, who may see everything
	 * @param filter   the narrowing the caller asked for
	 * @param search   the quoted term to text search for, or null
	 * @param pageable the page to return
	 * @param type     the projection to read the results as
	 * @return that page, knowing whether there is another one after it
	 */
	private <T> Slice<T> findSlice(Criteria scope, TestListFilter filter, String search, Pageable pageable, Class<T> type) {

		List<T> results = mongoTemplate.query(TestInfo.class)
				.inCollection(COLLECTION)
				.as(type)
				.matching(listingQuery(scope, filter, search, pageable))
				.all();

		return DBTestPlanService.slice(results, pageable);
	}

	/**
	 * @return the query of a listing: the filter and the scoping criteria, then the text search,
	 *         ordered and paged as asked, fetching one entry more than the page so that the slice
	 *         knows whether there is a next one
	 */
	static Query listingQuery(Criteria scope, TestListFilter filter, String search, Pageable pageable) {

		Query query = new Query(listingCriteria(scope, filter));

		if (search != null) {
			// added to the query rather than composed below, because TextCriteria is not a
			// Criteria and andOperator takes only those
			query.addCriteria(TextCriteria.forDefaultLanguage().matching(search));
		}

		query.with(pageable);
		query.limit(pageable.getPageSize() + 1);

		return query;
	}

	/**
	 * What the caller may see at all, narrowed by what they asked for. Composed with {@code $and}
	 * for the same reason the plan listing is: it composes anything with anything, and can only
	 * narrow, so scoping still wins whatever a filter asks for.
	 *
	 * @param scope  what the caller may see at all, or null for an admin who may see everything
	 * @param filter the narrowing the caller asked for
	 * @return those criteria; matches everything when there is nothing to narrow by
	 */
	static Criteria listingCriteria(Criteria scope, TestListFilter filter) {

		List<Criteria> parts = new ArrayList<>();

		if (!filter.isEmpty()) {
			parts.add(filter.toCriteria());
		}
		if (scope != null) {
			parts.add(scope);
		}

		return switch (parts.size()) {
			case 0 -> new Criteria();
			case 1 -> parts.get(0);
			default -> new Criteria().andOperator(parts.toArray(new Criteria[0]));
		};
	}

	/**
	 * Attach to every row the name of the plan it belongs to, looked up for the whole page in
	 * one query. A row whose plan cannot be found (deleted, or outside the scope) keeps no name.
	 *
	 * @param rows      one page of the listing
	 * @param planScope which plans may be named, or null for any
	 */
	private void attachPlanNames(List<? extends TestListRow> rows, Criteria planScope) {

		Set<String> planIds = rows.stream()
				.map(TestListRow::getPlanId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if (planIds.isEmpty()) {
			return;
		}

		Criteria criteria = Criteria.where("_id").in(planIds);
		Query query = new Query(planScope == null ? criteria : new Criteria().andOperator(criteria, planScope));
		query.fields().include("planName");

		Map<String, String> names = new HashMap<>();
		for (Document plan : mongoTemplate.find(query, Document.class, DBTestPlanService.COLLECTION)) {
			String name = plan.getString("planName");
			if (name != null) {
				names.put(String.valueOf(plan.get("_id")), name);
			}
		}
		applyPlanNames(rows, names);
	}

	/**
	 * @param rows  one page of the listing
	 * @param names plan id to plan name, for the plans that could be found
	 */
	static void applyPlanNames(List<? extends TestListRow> rows, Map<String, String> names) {
		for (TestListRow row : rows) {
			String name = row.getPlanId() == null ? null : names.get(row.getPlanId());
			if (name != null) {
				row.setPlanName(name);
			}
		}
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

		// The statistics page wants a {started: 1} index on this collection too, and it is
		// deliberately not created here: this runs before the server accepts its first
		// request, and building that index on a production sized TEST_INFO takes long enough
		// that the pod would fail its liveness probe and be restarted before it finished.
		// DBStatisticsService builds it in the background instead.
	}

	@Override
	public boolean deleteTests(List<String> ids) {
		// an admin may delete anyone's; everybody else only their own
		return deleteTests(ids, authenticationFacade.isAdmin() ? null : authenticationFacade.getPrincipal())
			.acknowledged();
	}

	@Override
	public Deleted deleteTests(List<String> ids, Map<String, String> owner) {

		// TEST_INFO is keyed by '_id' (TestInfo sets 'testId' to the same value, but only '_id'
		// is indexed, so filtering on 'testId' collection-scans). EVENT_LOG entries have their
		// own '_id' of testId + '-' + random, so they have to be matched on the (indexed)
		// 'testId' field instead.
		Criteria testInfoCriteria = Criteria.where("_id").in(ids);
		Criteria eventLogCriteria = Criteria.where("testId").in(ids);

		if (owner != null) {
			// TEST_INFO stores the owner under 'owner', EVENT_LOG entries under 'testOwner';
			// filtering EVENT_LOG on 'owner' matches nothing and orphans the log entries
			testInfoCriteria.and("owner").is(owner);
			eventLogCriteria.and("testOwner").is(owner);
		}

		DeleteResult testInfoDeleteResult = mongoTemplate.remove(new Query(testInfoCriteria), COLLECTION);
		DeleteResult logDeleteResult = mongoTemplate.remove(new Query(eventLogCriteria), DBEventLog.COLLECTION);

		// Drop any cached owner for the deleted tests, otherwise a warm cache (e.g. from an
		// earlier image-endpoint call) keeps authorising the owner for up to 30 minutes and
		// lets them re-create orphaned EVENT_LOG rows against the now-deleted test id.
		ids.forEach(testOwnerCache::invalidate);

		return new Deleted(testInfoDeleteResult.getDeletedCount(), logDeleteResult.getDeletedCount(),
			testInfoDeleteResult.wasAcknowledged() && logDeleteResult.wasAcknowledged());
	}
}
