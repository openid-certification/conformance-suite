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
import net.openid.conformance.sharing.SharedAsset;
import net.openid.conformance.variant.VariantSelection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.SortedMap;
import java.util.TreeMap;

@Service
public class DBTestPlanService implements TestPlanService {

	public static final String COLLECTION = "TEST_PLAN";

	/** The fields the scoping of a listing is expressed with, which a filter may not touch. */
	private static final Set<String> SCOPING_FIELDS = Set.of("owner", "publish");

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
	public void createTestPlan(String id, String planName, VariantSelection variant, JsonObject config, String description, List<String> certificationProfileName, List<Plan.Module> testModules, String summary, String publish) {

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
	public List<String> getTestPlanTestIds(String id) {
		Plan plan = getTestPlan(id);
		if (plan == null) {
			return new ArrayList<String>();
		}

		return plan.getModules().stream()
			.flatMap(module -> module.getInstances().stream())
			.collect(Collectors.toList());
	}

	@Override
	public Plan getTestPlan(String id) {

		if (!authenticationFacade.isAdmin()) {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			Plan plan = plans.findByIdAndOwner(id, owner).orElse(null);

			if (plan != null && authenticationFacade.isPrivateLinkUser()) {
				// Reject plans other than that referenced in the shared asset.
				SharedAsset sharedAsset = authenticationFacade.getPrivateOneTimeToken().getSharedAsset();
				if (sharedAsset == null || ! sharedAsset.getPlanId().equals(id)) {
					plan = null;
				}
			}

			return plan;
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
	public PaginationResponse<Plan> getPaginatedPlansForCurrentUser(PaginationRequest page, PlanListFilter filter,
																	String owner) {

		Map<String, String> principal = authenticationFacade.isAdmin() ? null : authenticationFacade.getPrincipal();

		if (!filter.isEmpty() || owner != null) {
			Criteria scope = ownerScope(principal, owner);
			return page.getSliceResponse((search, pageable) ->
					findSlice(scope, filter, search, pageable, Plan.class));
		}
		if (principal != null) {
			return page.getSliceResponse(
					p -> plans.findAllByOwnerAsSlice(principal, p),
					(s, p) -> plans.findAllByOwnerSearchAsSlice(principal, s, p));
		}
		return page.getSliceResponse(
				p -> plans.findAllAsSlice(p),
				(s, p) -> plans.findAllSearchAsSlice(s, p));
	}

	/**
	 * The criteria that decide which plans a listing may show at all. Narrowing to an
	 * {@code owner} is expressed <b>here</b> rather than in {@link PlanListFilter}, because
	 * {@code owner} is one of the {@link #SCOPING_FIELDS} a filter may never touch - see
	 * {@link #rejectScopingFields}. Doing it here also means it cannot widen anything: for
	 * anyone but an admin the caller's own principal is still required, so naming another
	 * owner lists nothing rather than that owner's plans.
	 *
	 * @param principal whose plans the caller may see at all, or null for an admin, who may
	 *                  see everyone's
	 * @param owner     the {@code owner.sub} the caller asked to narrow to, or null
	 * @return those criteria, or null when an admin asked for no narrowing and so may see
	 *         every plan
	 */
	static Criteria ownerScope(Map<String, String> principal, String owner) {

		Criteria mine = principal == null ? null : Criteria.where("owner").is(principal);
		Criteria asked = owner == null ? null : Criteria.where("owner.sub").is(owner);

		if (mine == null) {
			return asked;
		}
		if (asked == null) {
			return mine;
		}
		// $and rather than one document, because both clauses are about the owner field and
		// merging them would silently drop one
		return new Criteria().andOperator(mine, asked);
	}

	@Override
	public PaginationResponse<PublicPlan> getPaginatedPublicPlans(PaginationRequest page, PlanListFilter filter,
																	String owner) {

		if (!filter.isEmpty() || owner != null) {
			Criteria scope = owner == null ? published()
					: new Criteria().andOperator(published(), Criteria.where("owner.sub").is(owner));
			return page.getSliceResponse((search, pageable) ->
					findSlice(scope, filter, search, pageable, PublicPlan.class));
		}
		return page.getSliceResponse(
				p -> plans.findAllPublicAsSlice(p),
				(s, p) -> plans.findAllPublicSearchAsSlice(s, p));
	}

	/**
	 * Runs a filtered listing. Results are read through {@code Plan} as {@code type}, so a
	 * listing is projected in the database to the fields that listing may show - a public
	 * listing asks for {@link PublicPlan}, which has no owner and no configuration, exactly as
	 * the repository query it stands in for.
	 *
	 * @param scope    the criteria that decide what the caller may see at all, or null for an
	 *                 admin, who may see everything
	 * @param filter   the narrowing the caller asked for
	 * @param search   the quoted term to text search for, or null
	 * @param pageable the page to return
	 * @param type     the projection to read the results as
	 * @return that page, knowing whether there is another one after it
	 */
	private <T> Slice<T> findSlice(Criteria scope, PlanListFilter filter, String search, Pageable pageable, Class<T> type) {

		List<T> results = mongoTemplate.query(Plan.class)
				.inCollection(COLLECTION)
				.as(type)
				.matching(listingQuery(scope, filter, search, pageable))
				.all();

		return slice(results, pageable);
	}

	/**
	 * @param results  one page of results, plus the one extra entry {@link #listingQuery} asked
	 *                 for
	 * @param pageable the page they were fetched for
	 * @return the page itself, knowing whether there is another one after it - which is how
	 *         Spring Data builds a slice too, so that no count query is ever run
	 */
	static <T> Slice<T> slice(List<T> results, Pageable pageable) {

		boolean hasNext = results.size() > pageable.getPageSize();

		return new SliceImpl<>(hasNext ? results.subList(0, pageable.getPageSize()) : results, pageable, hasNext);
	}

	/**
	 * @return criteria matching the plans anyone may see, published either as a summary or in
	 *         full; a new instance every time, because criteria are mutable
	 */
	static Criteria published() {
		return Criteria.where("publish").in("summary", "everything");
	}

	/**
	 * @return the query of a filtered listing: the filter, the text search, and the scoping
	 *         criteria last, ordered and paged as asked, fetching one entry more than the page
	 */
	static Query listingQuery(Criteria scope, PlanListFilter filter, String search, Pageable pageable) {

		Query query = new Query();

		if (!filter.isEmpty()) {
			Criteria criteria = filter.toCriteria();
			rejectScopingFields(criteria.getCriteriaObject());
			query.addCriteria(criteria);
		}
		if (search != null) {
			// the term arrives quoted, so this is the same $text search the unfiltered
			// listing runs through PlanRepository
			query.addCriteria(TextCriteria.forDefaultLanguage().matching(search));
		}
		if (scope != null) {
			// last, because Query.getQueryObject() merges the criteria documents in the order
			// they were added: whatever decides what the caller may see has to be the winner
			// of any collision, not the filter the caller sent
			query.addCriteria(scope);
		}

		query.with(pageable);
		query.limit(pageable.getPageSize() + 1);

		return query;
	}

	/**
	 * A listing filter narrows a listing; it may never have an opinion on the fields that
	 * decide whose plans are listed. No {@link PlanListFilter} can produce one today - this is
	 * here so that a filter that grows a new field can never quietly become a way to widen a
	 * listing, given that the merge of criteria documents cannot report a collision itself.
	 *
	 * @param criteria the filter's criteria
	 * @throws IllegalStateException if it touches a field the scoping is expressed with
	 */
	static void rejectScopingFields(Document criteria) {
		for (String field : SCOPING_FIELDS) {
			if (criteria.containsKey(field)) {
				throw new IllegalStateException(
						"a plan listing filter must not filter on '" + field + "', which is what scopes the listing");
			}
		}
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

		// Drill-down listing filters: plans of one plan name over a period, and by period alone.
		collection.createIndex(new Document("planName", 1).append("started", -1));
		collection.createIndex(new Document("started", -1));

		// Listing (and deleting) one account's plans over a period. Measured against a copy of
		// production, where one owner has 57,845 plans older than a year: counting them went from
		// 7.4 seconds to 16 milliseconds, and the whole bulk-delete preview from 12.2 seconds to
		// 1.4. Costs 18 MB on a 2.5 GB collection.
		collection.createIndex(new Document("owner.sub", 1).append("started", -1));
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
