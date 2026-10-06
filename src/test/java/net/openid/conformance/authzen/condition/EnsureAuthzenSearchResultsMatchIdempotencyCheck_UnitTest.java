package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
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
		JsonObject response = new JsonObject();
		response.add("body_json", JsonParser.parseString(currentJson));
		env.putObject("authzen_api_endpoint_response", response);
		env.putString("authzen_idempotency_first_response_body", firstJson);
	}

	@Test
	public void searchResultsSameOrder_succeeds() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void searchResultsReordered_succeeds() {
		// Multiset semantics: order does not matter.
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""",
			"""
			{ "results": [ { "type": "user", "id": "bob" }, { "type": "user", "id": "alice" } ] }""");
		cond.execute(env);
	}

	@Test
	public void searchResultsSizeChanged_fails() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void searchResultsDuplicateMultiplicityDiffers_fails() {
		// [A,A,B] vs [A,B,B] have equal size and equal sets, but different multisets.
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" }, { "type": "user", "id": "bob" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void searchPageNextTokenDiffers_succeeds() {
		// page.next_token is opaque; differing tokens across iterations are not idempotency violations.
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": { "next_token": "tok-1" } }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": { "next_token": "tok-2" } }""");
		cond.execute(env);
	}

	@Test
	public void searchPageNextTokenAddedAcrossIterations_fails() {
		// Iteration 1 had no next_token (no more pages); iteration 2 has one (more pages).
		// The pagination "more available?" signal flipped — PDP is not idempotent.
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": {} }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": { "next_token": "tok-2" } }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void searchPageNextTokenDroppedAcrossIterations_fails() {
		// Inverse: iteration 1 had a next_token; iteration 2 lost it.
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": { "next_token": "tok-1" } }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": {} }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void searchPageEmptyStringTokenEqualsAbsent_succeeds() {
		// Section 8.3: an empty-string next_token means "no more pages",
		// semantically identical to the field being absent. Presence-equality
		// must treat them as the same observable.
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": {} }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": { "next_token": "" } }""");
		cond.execute(env);
	}

	@Test
	public void searchPageTotalDiffers_succeeds() {
		// Section 8.2.2: page.total may be an estimate, so it is not part of the answer.
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": { "next_token": "tok-1", "total": 10 } }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": { "next_token": "tok-2", "total": 11 } }""");
		cond.execute(env);
	}

	@Test
	public void searchContextDiffers_succeeds() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ], "context": { "query_execution_time_ms": 42 } }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" } ], "context": { "query_execution_time_ms": 37 } }""");
		cond.execute(env);
	}

	@Test
	public void currentBodyWithoutResults_fails() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ] }""",
			"""
			{ "decision": true }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingCurrentBody_fails() {
		env.putObject("authzen_api_endpoint_response", new JsonObject());
		env.putString("authzen_idempotency_first_response_body", "{ \"results\": [] }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
	@Test
	public void sameResultsWithDifferentMemberOrder_succeeds() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "bob" }, { "id": "alice", "type": "user" } ] }""",
			"""
			{ "results": [ { "id": "bob", "type": "user" }, { "type": "user", "id": "alice" } ] }""");
		cond.execute(env);
	}
}
