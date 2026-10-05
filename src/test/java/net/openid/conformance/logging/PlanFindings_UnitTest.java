package net.openid.conformance.logging;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class PlanFindings_UnitTest {

	private static final List<PlanFindings.Run> RUNS = List.of(
		new PlanFindings.Run(0, "module-a", Map.of("v", "1"), "test-a"),
		new PlanFindings.Run(2, "module-c", Map.of("v", "1"), "test-c"),
		new PlanFindings.Run(3, "module-d", Map.of("v", "2"), "test-d"));

	@Test
	void aFindingLoggedBySeveralModulesIsListedOnceWithEachOfThem() {
		PlanFindings result = PlanFindings.of(RUNS, List.of(
			entry("c-1", "test-c", 10, "FAILURE", "CheckIssuer", "issuer mismatch", "OIDCC-2"),
			entry("a-1", "test-a", 20, "FAILURE", "CheckIssuer", "issuer mismatch", "OIDCC-2", "RFC8414-3")));

		assertThat(result.findings()).hasSize(1);
		PlanFindings.Finding finding = result.findings().get(0);
		assertThat(finding.kind()).isEqualTo("FAILURE");
		assertThat(finding.src()).isEqualTo("CheckIssuer");
		assertThat(finding.msg()).isEqualTo("issuer mismatch");
		assertThat(finding.requirements()).containsExactly("OIDCC-2", "RFC8414-3");
		// in plan order, whatever order the entries arrived in
		assertThat(finding.occurrences()).containsExactly(
			new PlanFindings.Occurrence(0, "module-a", Map.of("v", "1"), "test-a", "a-1", 1),
			new PlanFindings.Occurrence(2, "module-c", Map.of("v", "1"), "test-c", "c-1", 1));
	}

	@Test
	void aFindingRepeatedWithinOneRunIsOneOccurrencePointingAtItsFirstEntry() {
		PlanFindings result = PlanFindings.of(RUNS, List.of(
			entry("a-late", "test-a", 30, "WARNING", "CheckKeys", "weak key"),
			entry("a-early", "test-a", 10, "WARNING", "CheckKeys", "weak key")));

		assertThat(result.findings()).hasSize(1);
		assertThat(result.findings().get(0).occurrences()).containsExactly(
			new PlanFindings.Occurrence(0, "module-a", Map.of("v", "1"), "test-a", "a-early", 2));
	}

	@Test
	void theSameConditionWithADifferentMessageOrResultIsADifferentFinding() {
		PlanFindings result = PlanFindings.of(RUNS, List.of(
			entry("a-1", "test-a", 10, "FAILURE", "CheckIssuer", "issuer mismatch"),
			entry("a-2", "test-a", 20, "FAILURE", "CheckIssuer", "issuer missing"),
			entry("a-3", "test-a", 30, "WARNING", "CheckIssuer", "issuer mismatch")));

		assertThat(result.findings()).extracting(PlanFindings.Finding::kind, PlanFindings.Finding::msg)
			.containsExactly(
				tuple("FAILURE", "issuer mismatch"),
				tuple("FAILURE", "issuer missing"),
				tuple("WARNING", "issuer mismatch"));
	}

	@Test
	void failuresComeBeforeWarningsBeforeImagesAndTheMostWidespreadFirst() {
		PlanFindings result = PlanFindings.of(RUNS, List.of(
			entry("a-img", "test-a", 5, "REVIEW", "ExpectErrorPage", "error page shown"),
			entry("a-w", "test-a", 10, "WARNING", "CheckKeys", "weak key"),
			entry("a-f1", "test-a", 20, "FAILURE", "OnlyInA", "only in a"),
			entry("a-f2", "test-a", 30, "FAILURE", "Everywhere", "everywhere"),
			entry("c-f2", "test-c", 30, "FAILURE", "Everywhere", "everywhere"),
			entry("d-f2", "test-d", 30, "FAILURE", "Everywhere", "everywhere")));

		assertThat(result.findings()).extracting(PlanFindings.Finding::kind, PlanFindings.Finding::src)
			.containsExactly(
				tuple("FAILURE", "Everywhere"),
				tuple("FAILURE", "OnlyInA"),
				tuple("WARNING", "CheckKeys"),
				tuple("IMAGE", "ExpectErrorPage"));
	}

	@Test
	void anImageAddedOnTheUploadPageHasNoResultOrMessageAndIsStillAnImage() {
		PlanFindings result = PlanFindings.of(RUNS, List.of(
			new Document("_id", "d-free").append("testId", "test-d").append("time", 10L).append("src", "_image-api")));

		assertThat(result.findings()).hasSize(1);
		PlanFindings.Finding finding = result.findings().get(0);
		assertThat(finding.kind()).isEqualTo("IMAGE");
		assertThat(finding.msg()).isNull();
		assertThat(finding.requirements()).isEmpty();
		assertThat(finding.occurrences()).extracting(PlanFindings.Occurrence::entryId).containsExactly("d-free");
	}

	@Test
	void aReviewEntryTheBrowserAutomationSatisfiedIsACapturedPageNotAnImage() {
		PlanFindings result = PlanFindings.of(RUNS, List.of(
			entry("a-page", "test-a", 10, "REVIEW", "ExpectErrorPage", "error page shown")
				.append("content_type", "text/html"),
			entry("c-page", "test-c", 10, "REVIEW", "ExpectErrorPage", "error page shown")
				.append("content_type", null),
			entry("d-img", "test-d", 10, "REVIEW", "ExpectErrorPage", "error page shown")));

		// the same placeholder satisfied two ways is two findings, images first
		assertThat(result.findings()).extracting(PlanFindings.Finding::kind, f -> f.occurrences().size())
			.containsExactly(tuple("IMAGE", 1), tuple("PAGE", 2));
	}

	@Test
	void eachRunsEarliestFailureIsNamedWhateverGroupItFallsIn() {
		PlanFindings result = PlanFindings.of(RUNS, List.of(
			// test-c's earliest failure is the widespread one; test-a's is its own, logged first
			entry("a-own", "test-a", 10, "FAILURE", "OnlyInA", "only in a"),
			entry("a-shared", "test-a", 20, "FAILURE", "Everywhere", "everywhere"),
			entry("c-shared", "test-c", 5, "FAILURE", "Everywhere", "everywhere"),
			entry("c-own", "test-c", 30, "FAILURE", "OnlyInC", "only in c"),
			entry("d-warn", "test-d", 10, "WARNING", "CheckKeys", "weak key")));

		// a warning is not a failure, so test-d has none
		assertThat(result.firstFailures()).containsExactly(
			new PlanFindings.FirstFailure(0, "test-a", "a-own"),
			new PlanFindings.FirstFailure(2, "test-c", "c-shared"));
	}

	@Test
	void entriesOfRunsThatAreNotTheLatestOfAModuleAreIgnored() {
		PlanFindings result = PlanFindings.of(RUNS, List.of(
			entry("old-1", "test-a-earlier-run", 10, "FAILURE", "CheckIssuer", "issuer mismatch")));

		assertThat(result.findings()).isEmpty();
	}

	private static Document entry(String id, String testId, long time, String result, String src, String msg,
								  String... requirements) {
		Document entry = new Document("_id", id).append("testId", testId).append("time", time)
			.append("result", result).append("src", src).append("msg", msg);
		if (requirements.length > 0) {
			entry.append("requirements", List.of(requirements));
		}
		return entry;
	}
}
