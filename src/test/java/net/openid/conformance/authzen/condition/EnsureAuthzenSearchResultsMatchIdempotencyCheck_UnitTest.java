package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EnsureAuthzenSearchResultsMatchIdempotencyCheck_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureAuthzenSearchResultsMatchIdempotencyCheck cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureAuthzenSearchResultsMatchIdempotencyCheck();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putFirstAndCurrent(String firstJson, String currentJson) {
		env.putObject("authzen_idempotency_first_search_results", JsonParser.parseString(firstJson).getAsJsonObject());
		env.putObject("authzen_search_endpoint_aggregated_results", JsonParser.parseString(currentJson).getAsJsonObject());
	}

	@Test
	public void sameResultsSameOrder_succeeds() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""", """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void sameResultsReordered_succeeds() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""", """
			{ "results": [ { "type": "user", "id": "bob" }, { "type": "user", "id": "alice" } ] }""");
		cond.execute(env);
	}

	@Test
	public void sameResultsWithDifferentMemberOrder_succeeds() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ] }""", """
			{ "results": [ { "id": "alice", "type": "user" } ] }""");
		cond.execute(env);
	}

	@Test
	public void resultCountChanged_fails() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""", """
			{ "results": [ { "type": "user", "id": "alice" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void resultChanged_fails() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ] }""", """
			{ "results": [ { "type": "user", "id": "carol" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void duplicateMultiplicityDiffers_fails() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""", """
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" }, { "type": "user", "id": "bob" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void emptyResultsBothTimes_succeeds() {
		putFirstAndCurrent("""
			{ "results": [] }""", """
			{ "results": [] }""");
		cond.execute(env);
	}

	@Test
	public void currentWithoutResults_fails() {
		putFirstAndCurrent("""
			{ "results": [] }""", """
			{ }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingCurrent_fails() {
		env.putObject("authzen_idempotency_first_search_results", JsonParser.parseString("""
			{ "results": [] }""").getAsJsonObject());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
