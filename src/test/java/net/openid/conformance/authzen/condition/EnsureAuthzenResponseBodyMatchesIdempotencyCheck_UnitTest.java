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
class EnsureAuthzenResponseBodyMatchesIdempotencyCheck_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureAuthzenResponseBodyMatchesIdempotencyCheck cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureAuthzenResponseBodyMatchesIdempotencyCheck();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putFirstAndCurrent(String firstJson, String currentJson) {
		JsonObject response = new JsonObject();
		response.add("body_json", JsonParser.parseString(currentJson));
		env.putObject("authzen_api_endpoint_response", response);
		env.putString("authzen_idempotency_first_response_body", firstJson);
	}

	@Test
	public void evaluationDecisionBodyMatches_succeeds() {
		putFirstAndCurrent("""
			{ "decision": true }""",
			"""
			{ "decision": true }""");
		cond.execute(env);
	}

	@Test
	public void evaluationDecisionBodyDiffers_fails() {
		putFirstAndCurrent("""
			{ "decision": true }""",
			"""
			{ "decision": false }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
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
	public void searchPageOtherFieldDiffers_fails() {
		// Non-token page fields are still compared strictly.
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": { "next_token": "tok-1", "total": 10 } }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" } ], "page": { "next_token": "tok-2", "total": 11 } }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void searchOtherTopLevelFieldDiffers_fails() {
		putFirstAndCurrent("""
			{ "results": [ { "type": "user", "id": "alice" } ], "context": { "v": 1 } }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" } ], "context": { "v": 2 } }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void missingCurrentBody_fails() {
		env.putObject("authzen_api_endpoint_response", new JsonObject());
		env.putString("authzen_idempotency_first_response_body", "{ \"decision\": true }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
