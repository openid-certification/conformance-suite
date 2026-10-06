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
class EnsureSearchResponsePageCountMatchesResults_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureSearchResponsePageCountMatchesResults cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureSearchResponsePageCountMatchesResults();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putResponse(String json) {
		env.putObject("authzen_search_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void noCount_succeeds() {
		putResponse("""
			{ "results": [ { "name": "read" } ], "page": { "next_token": "" } }""");
		cond.execute(env);
	}

	@Test
	public void noPage_succeeds() {
		putResponse("""
			{ "results": [ { "name": "read" } ] }""");
		cond.execute(env);
	}

	@Test
	public void countMatches_succeeds() {
		putResponse("""
			{ "results": [ { "name": "read" }, { "name": "write" } ], "page": { "next_token": "", "count": 2 } }""");
		cond.execute(env);
	}

	@Test
	public void countDiffers_throws() {
		putResponse("""
			{ "results": [ { "name": "read" } ], "page": { "next_token": "t", "count": 2 } }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("page.count does not equal"));
	}

	@Test
	public void countBeyondIntRange_doesNotWrapToTheResultCount() {
		// 2^32 + 1 wraps to 1 as an int, which would equal the one result.
		putResponse("""
			{ "results": [ { "name": "read" } ], "page": { "next_token": "", "count": 4294967297 } }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("page.count does not equal"));
	}

	@Test
	public void countBeyondLongRange_fails() {
		putResponse("""
			{ "results": [ { "name": "read" } ], "page": { "next_token": "", "count": 18446744073709551617 } }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void countWrittenWithAFraction_isComparedByValue() {
		// Whether count is an integer is EnsureValidSearchResponsePage's check; 2.0 is two results.
		putResponse("""
			{ "results": [ { "name": "read" }, { "name": "write" } ], "page": { "next_token": "", "count": 2.0 } }""");
		cond.execute(env);
	}

	@Test
	public void countNotANumber_fails() {
		putResponse("""
			{ "results": [ { "name": "read" } ], "page": { "next_token": "", "count": "1" } }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("page.count is not a number"));
	}
}
