package net.openid.conformance.info;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The listing query built for {@code GET /api/plan}. Everything asserted here is decided
 * without touching a database.
 */
class DBTestPlanService_UnitTest {

	private static final PlanListFilter NO_FILTER = new PlanListFilter(null, Map.of(), null, null, null, null);

	private static final Map<String, String> OWNER = Map.of("sub", "developer", "iss", "https://developer.com");

	private static final Pageable SECOND_PAGE = PageRequest.of(1, 10, Sort.by(Sort.Order.desc("started")));

	@Test
	void anAdminWithoutFiltersQueriesTheWholeCollection() {
		Query query = DBTestPlanService.listingQuery(null, NO_FILTER, null, SECOND_PAGE);

		assertThat(query.getQueryObject()).isEmpty();
		assertThat(query.getSortObject()).isEqualTo(new Document("started", -1));
		assertThat(query.getSkip()).isEqualTo(10);
		// one more than the page, which is how the slice knows there is a next page
		assertThat(query.getLimit()).isEqualTo(11);
	}

	@Test
	void theScopingCriteriaAreKept() {
		Query owned = DBTestPlanService.listingQuery(Criteria.where("owner").is(OWNER), NO_FILTER, null, SECOND_PAGE);
		Query published = DBTestPlanService.listingQuery(DBTestPlanService.published(), NO_FILTER, null, SECOND_PAGE);

		assertThat(owned.getQueryObject()).isEqualTo(new Document("owner", OWNER));
		assertThat(published.getQueryObject())
			.isEqualTo(new Document("publish", new Document("$in", List.of("summary", "everything"))));
	}

	@Test
	void theFilterNarrowsWithinTheScoping() {
		PlanListFilter filter = new PlanListFilter(Set.of("fapi-ciba-id1-test-plan"),
			Map.of("fapi_profile", "openbanking_brazil"), null, null, "2026-06-01", "2026-07-01");

		Query query = DBTestPlanService.listingQuery(Criteria.where("owner").is(OWNER), filter, null, SECOND_PAGE);

		assertThat(query.getQueryObject()).isEqualTo(new Document()
			.append("owner", OWNER)
			.append("planName", "fapi-ciba-id1-test-plan")
			.append("variant.fapi_profile", "openbanking_brazil")
			.append("started", new Document("$gte", "2026-06-01").append("$lt", "2026-07-01")));
	}

	@Test
	void searchingIsTheSameTextSearchTheUnfilteredListingUses() {
		PlanListFilter filter = new PlanListFilter(null, Map.of(), "FAPI-CIBA: Poll w/ MTLS", null, null, null);

		Query query = DBTestPlanService.listingQuery(DBTestPlanService.published(), filter, "\"ciba\"", SECOND_PAGE);

		assertThat(query.getQueryObject()).isEqualTo(new Document()
			.append("publish", new Document("$in", List.of("summary", "everything")))
			.append("certificationProfileName", "FAPI-CIBA: Poll w/ MTLS")
			.append("$text", new Document("$search", "\"ciba\"")));
	}

	@Test
	void theScopingWinsIfAFilterEverCollidesWithIt() {
		// no PlanListFilter can produce an owner or publish clause, so the collision is staged
		// on the one field both can name: the scoping is added last, so the scoping is what
		// the merged query document keeps
		PlanListFilter filter = new PlanListFilter(Set.of("some-other-plan"), Map.of(), null, null, null, null);

		Query query = DBTestPlanService.listingQuery(
			Criteria.where("planName").is("the-only-plan-you-may-see"), filter, null, SECOND_PAGE);

		assertThat(query.getQueryObject()).isEqualTo(new Document("planName", "the-only-plan-you-may-see"));
	}

	@Test
	void aFilterOnAScopingFieldIsRejected() {
		assertThatThrownBy(() -> DBTestPlanService.rejectScopingFields(new Document("owner", Map.of("sub", "someone"))))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("owner");
		assertThatThrownBy(() -> DBTestPlanService.rejectScopingFields(new Document("publish", "everything")))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("publish");

		assertThatCode(() -> DBTestPlanService.rejectScopingFields(new PlanListFilter(Set.of("a-plan"),
			Map.of("fapi_profile", "plain"), "a profile", true, "2026-06-01", "2026-07-01").toCriteria().getCriteriaObject()))
			.doesNotThrowAnyException();
	}

	@Test
	void aFullPageKnowsThereIsAnotherOneAndDoesNotShowTheEntryItPeekedAt() {
		Slice<String> slice = DBTestPlanService.slice(
			new ArrayList<>(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")), PageRequest.of(0, 10));

		assertThat(slice.hasNext()).isTrue();
		assertThat(slice.getContent()).hasSize(10).endsWith("j");
	}

	@Test
	void aPageThatIsNotFullIsTheLastOne() {
		Slice<String> slice = DBTestPlanService.slice(new ArrayList<>(List.of("a", "b")), PageRequest.of(0, 10));

		assertThat(slice.hasNext()).isFalse();
		assertThat(slice.getContent()).containsExactly("a", "b");

		assertThat(DBTestPlanService.slice(List.<String>of(), PageRequest.of(0, 10)).hasNext()).isFalse();
	}

	@Test
	void aFamilyWithoutAnyPlansListsNothing() {
		PlanListFilter filter = new PlanListFilter(Set.of(), Map.of(), null, null, null, null);

		Query query = DBTestPlanService.listingQuery(null, filter, null, SECOND_PAGE);

		assertThat(query.getQueryObject()).isEqualTo(new Document("planName", new Document("$in", List.of())));
	}

	@Test
	void anAdminWhoNarrowsToNoOwnerSeesEveryPlan() {
		assertThat(DBTestPlanService.ownerScope(null, null)).isNull();
	}

	@Test
	void anAdminCanNarrowAListingToOneOwner() {
		assertThat(DBTestPlanService.ownerScope(null, "developer").getCriteriaObject())
			.isEqualTo(new Document("owner.sub", "developer"));
	}

	@Test
	void anyoneElseIsScopedToTheirOwnPlansAsBefore() {
		assertThat(DBTestPlanService.ownerScope(OWNER, null).getCriteriaObject())
			.isEqualTo(new Document("owner", new Document(OWNER)));
	}

	@Test
	void namingAnOwnerCannotWidenWhatAnyoneElseMaySee() {
		// both clauses survive, so asking for someone else's plans lists nothing at all rather
		// than listing theirs; merging them into one document would drop the caller's own
		Document criteria = DBTestPlanService.ownerScope(OWNER, "somebody-else").getCriteriaObject();

		assertThat(criteria).isEqualTo(new Document("$and", List.of(
			new Document("owner", new Document(OWNER)),
			new Document("owner.sub", "somebody-else"))));
	}
}
