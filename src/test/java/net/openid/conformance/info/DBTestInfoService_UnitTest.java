package net.openid.conformance.info;

import com.google.gson.JsonObject;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The listing query built for {@code GET /api/log}, and how a page of it is completed with the
 * names of the plans its tests belong to. Everything asserted here is decided without touching a
 * database.
 */
class DBTestInfoService_UnitTest {

	private static final Map<String, String> OWNER = Map.of("sub", "developer", "iss", "https://developer.com");

	private static final Pageable SECOND_PAGE = PageRequest.of(1, 25, Sort.by(Sort.Order.desc("started")));

	@Test
	void anAdminWithoutFiltersQueriesTheWholeCollection() {
		Query query = DBTestInfoService.listingQuery(null, TestListFilter.NONE, null, SECOND_PAGE);

		assertThat(query.getQueryObject()).isEmpty();
		assertThat(query.getSortObject()).isEqualTo(new Document("started", -1));
		assertThat(query.getSkip()).isEqualTo(25);
		// one more than the page, which is how the slice knows there is a next page
		assertThat(query.getLimit()).isEqualTo(26);
	}

	@Test
	void theScopingCriteriaAreKept() {
		Query owned = DBTestInfoService.listingQuery(Criteria.where("owner").is(OWNER), TestListFilter.NONE, null, SECOND_PAGE);
		Query published = DBTestInfoService.listingQuery(DBTestPlanService.published(), TestListFilter.NONE, null, SECOND_PAGE);

		assertThat(owned.getQueryObject()).isEqualTo(new Document("owner", OWNER));
		assertThat(published.getQueryObject())
			.isEqualTo(new Document("publish", new Document("$in", List.of("summary", "everything"))));
	}

	@Test
	void theFilterNarrowsWithinTheScoping() {
		TestListFilter filter = TestListFilter.parse("running,waiting", "unknown");

		Query query = DBTestInfoService.listingQuery(Criteria.where("owner").is(OWNER), filter, null, SECOND_PAGE);

		// $and, not one merged document, so that the scoping wins whatever the filter asks for
		assertThat(query.getQueryObject()).isEqualTo(new Document("$and", List.of(
			new Document()
				.append("status", new Document("$in", List.of("RUNNING", "WAITING")))
				.append("result", new Document("$in", List.of("UNKNOWN"))),
			new Document("owner", OWNER))));
	}

	@Test
	void aFilterForAnAdminIsTheWholeQuery() {
		Query query = DBTestInfoService.listingQuery(null, TestListFilter.parse("finished", null), null, SECOND_PAGE);

		assertThat(query.getQueryObject())
			.isEqualTo(new Document("status", new Document("$in", List.of("FINISHED"))));
	}

	@Test
	void searchingIsATextSearchAlongsideTheFilter() {
		Query query = DBTestInfoService.listingQuery(DBTestPlanService.published(),
			TestListFilter.parse(null, "failed"), "\"rotate keys\"", SECOND_PAGE);

		// $text stays at the top level, where it is the only place Mongo allows it outside an $and
		assertThat(query.getQueryObject().get("$text")).isEqualTo(new Document("$search", "\"rotate keys\""));
		assertThat(query.getQueryObject().get("$and")).isEqualTo(List.of(
			new Document("result", new Document("$in", List.of("FAILED"))),
			new Document("publish", new Document("$in", List.of("summary", "everything")))));
	}

	@Test
	void eachRowGetsTheNameOfThePlanItBelongsToWhenThatPlanWasFound() {
		TestInfo inPlan = test("t1", "plan-1");
		TestInfo inSamePlan = test("t2", "plan-1");
		TestInfo planGone = test("t3", "plan-deleted");
		TestInfo standalone = test("t4", null);

		DBTestInfoService.applyPlanNames(List.of(inPlan, inSamePlan, planGone, standalone),
			Map.of("plan-1", "oidcc-basic-certification-test-plan", "plan-2", "some-other-plan"));

		assertThat(inPlan.getPlanName()).isEqualTo("oidcc-basic-certification-test-plan");
		assertThat(inSamePlan.getPlanName()).isEqualTo("oidcc-basic-certification-test-plan");
		assertThat(planGone.getPlanName()).isNull();
		assertThat(standalone.getPlanName()).isNull();
	}

	private static TestInfo test(String id, String planId) {
		return new TestInfo(id, "oidcc-server", null, Instant.parse("2026-09-01T10:00:00Z"), new JsonObject(),
			"", null, OWNER, planId, "1.0", "", null);
	}
}
