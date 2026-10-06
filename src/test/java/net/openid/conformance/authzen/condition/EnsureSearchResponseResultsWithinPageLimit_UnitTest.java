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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsureSearchResponseResultsWithinPageLimit_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureSearchResponseResultsWithinPageLimit cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureSearchResponseResultsWithinPageLimit();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String request, String response) {
		env.putObject("authzen_api_endpoint_request", JsonParser.parseString(request).getAsJsonObject());
		env.putObject("authzen_search_endpoint_response", JsonParser.parseString(response).getAsJsonObject());
	}

	@Test
	public void noLimit_succeeds() {
		put("""
			{ "subject": { "type": "user" } }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void resultsAtLimit_whilePaginating_succeeds() {
		put("""
			{ "page": { "limit": 2 } }""",
			"""
			{ "page": { "next_token": "a3M9NDU2O3N6PTI=" }, "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void resultsOverLimit_withNextToken_throws() {
		put("""
			{ "page": { "limit": 1 } }""",
			"""
			{ "page": { "next_token": "a3M9NDU2O3N6PTI=" }, "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("more results than"));
	}

	@Test
	public void resultsOverLimit_onLastPageOfPaginatedSearch_throws() {
		put("""
			{ "page": { "limit": 1, "token": "a3M9NDU2O3N6PTI=" } }""",
			"""
			{ "page": { "next_token": "" }, "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("more results than"));
	}

	@Test
	public void resultsOverLimit_withoutPageObject_isNotChecked() {
		// A PDP that does not paginate returns everything and ignores the page object (scenario 7.5.4).
		put("""
			{ "page": { "limit": 1 } }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void resultsOverLimit_withEmptyNextToken_isNotChecked() {
		put("""
			{ "page": { "limit": 1 } }""",
			"""
			{ "page": { "next_token": "" }, "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void nonNumericLimit_isNotChecked() {
		put("""
			{ "page": { "limit": "one" } }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}
}
