package net.openid.conformance.info;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a bulk delete would select. Everything asserted here is decided without touching a
 * database, and it is the only place the "never delete this" rules are written down.
 */
class BulkPlanDeleter_UnitTest {

	private static final PlanListFilter NO_FILTER = new PlanListFilter(null, Map.of(), null, null, null, null);

	/** The two rules that hold whatever was asked for. */
	private static final Document KEEP =
		new Document("immutable", new Document("$ne", true)).append("publish", null);

	@Test
	void aCertifiedOrPublishedPlanIsNeverSelected() {
		assertThat(BulkPlanDeleter.selection(null, NO_FILTER, null)).isEqualTo(KEEP);
	}

	@Test
	void thePlansAskedForAreSelectedAsWellAsTheOnesNeverDeleted() {
		PlanListFilter filter = new PlanListFilter(null, Map.of(), null, null, "2025-01-01", "2026-01-01");

		assertThat(BulkPlanDeleter.selection(null, filter, null)).isEqualTo(new Document("$and", List.of(
			new Document("started", new Document("$gte", "2025-01-01").append("$lt", "2026-01-01")),
			KEEP)));
	}

	@Test
	void whatTheCallerMaySeeIsSelectedTooAndCannotBeWidenedByTheFilter() {
		Criteria scope = Criteria.where("owner.sub").is("developer");

		assertThat(BulkPlanDeleter.selection(scope, NO_FILTER, null)).isEqualTo(new Document("$and", List.of(
			new Document("owner.sub", "developer"),
			KEEP)));
	}

	@Test
	void askingForImmutablePlansStillSelectsNoneOfThem() {
		// the filter says immutable=true, the rule says never; the rule has to win, and does
		// because both clauses are kept and no plan can satisfy them at once
		PlanListFilter filter = new PlanListFilter(null, Map.of(), null, true, null, null);

		assertThat(BulkPlanDeleter.selection(null, filter, null)).isEqualTo(new Document("$and", List.of(
			new Document("immutable", true),
			KEEP)));
	}

	@Test
	void theTestIdsToDeleteComeFromThePlanDocumentItself() {
		List<Document> plans = List.of(
			new Document("_id", "plan-1").append("modules", List.of(
				new Document("testModule", "a").append("instances", List.of("t1", "t2")),
				new Document("testModule", "b").append("instances", List.of("t3")))),
			new Document("_id", "plan-2").append("modules", List.of(
				new Document("testModule", "c").append("instances", List.of("t4")))));

		assertThat(BulkPlanDeleter.testIdsOf(plans)).containsExactly("t1", "t2", "t3", "t4");
	}

	@Test
	void aPlanWithNoModulesOrNoInstancesContributesNothing() {
		List<Document> plans = List.of(
			new Document("_id", "never-run"),
			new Document("_id", "module-never-run").append("modules", List.of(
				new Document("testModule", "a"))),
			new Document("_id", "empty").append("modules", List.of(
				new Document("testModule", "a").append("instances", List.of()))));

		assertThat(BulkPlanDeleter.testIdsOf(plans)).isEmpty();
	}

	@Test
	void aSearchedListingDeletesWhatThatSearchFound() {
		// the term arrives already quoted, exactly as PaginationRequest.searchTerm hands it to
		// the listing, so a delete searches for the same phrase the listing searched for
		Document selection = BulkPlanDeleter.selection(null, NO_FILTER, "\"ciba\"");

		assertThat(selection).isEqualTo(new Document("$and", List.of(
			new Document("$text", new Document("$search", "\"ciba\"")),
			KEEP)));
	}

	@Test
	void notSearchingForAnythingLeavesTheSelectionAlone() {
		assertThat(BulkPlanDeleter.selection(null, NO_FILTER, null)).isEqualTo(KEEP);
	}
}
