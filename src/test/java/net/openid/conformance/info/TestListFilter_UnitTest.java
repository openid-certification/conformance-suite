package net.openid.conformance.info;

import net.openid.conformance.testmodule.TestModule.Result;
import net.openid.conformance.testmodule.TestModule.Status;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The {@code status} and {@code result} parameters of {@code GET /api/log}, as the filter chips
 * on the logs page send them.
 */
class TestListFilter_UnitTest {

	@Test
	void nothingAskedForNarrowsNothing() {
		assertThat(TestListFilter.parse(null, null).isEmpty()).isTrue();
		assertThat(TestListFilter.parse("", " , ").isEmpty()).isTrue();
		assertThat(TestListFilter.parse("", "").toCriteria().getCriteriaObject()).isEmpty();
	}

	@Test
	void eachParameterIsACommaSeparatedListMatchedCaseInsensitively() {
		TestListFilter filter = TestListFilter.parse("running, Waiting", "FAILED,unknown");

		assertThat(filter.statuses()).containsExactlyInAnyOrder(Status.RUNNING, Status.WAITING);
		assertThat(filter.results()).containsExactlyInAnyOrder(Result.FAILED, Result.UNKNOWN);
		assertThat(filter.isEmpty()).isFalse();
	}

	@Test
	void theCriteriaAreOneInClausePerParameterWithTheNamesSorted() {
		TestListFilter filter = TestListFilter.parse("waiting,running", "unknown,failed");

		assertThat(filter.toCriteria().getCriteriaObject()).isEqualTo(new Document()
			.append("status", new Document("$in", List.of("RUNNING", "WAITING")))
			.append("result", new Document("$in", List.of("FAILED", "UNKNOWN"))));

		assertThat(TestListFilter.parse("finished", null).toCriteria().getCriteriaObject())
			.isEqualTo(new Document("status", new Document("$in", List.of("FINISHED"))));
		assertThat(TestListFilter.parse(null, "passed").toCriteria().getCriteriaObject())
			.isEqualTo(new Document("result", new Document("$in", List.of("PASSED"))));
	}

	@Test
	void aValueATestCannotHaveIsRefusedByName() {
		assertThatThrownBy(() -> TestListFilter.parse("running,bogus", null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("'bogus' is not a status");
		assertThatThrownBy(() -> TestListFilter.parse(null, "RUNNING"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("'RUNNING' is not a result");
	}

	@Test
	void theRecordCopiesWhatItIsGiven() {
		assertThat(new TestListFilter(null, null)).isEqualTo(TestListFilter.NONE);
		assertThat(new TestListFilter(Set.of(Status.RUNNING), null).results()).isEmpty();
	}
}
