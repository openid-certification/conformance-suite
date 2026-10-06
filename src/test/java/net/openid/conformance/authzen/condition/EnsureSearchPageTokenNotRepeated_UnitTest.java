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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsureSearchPageTokenNotRepeated_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureSearchPageTokenNotRepeated cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureSearchPageTokenNotRepeated();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	/** A page with results for the given subject ids that returns the given next_token ({@code null} for none). */
	private void pageReturned(String token, String... resultIds) {
		StringBuilder results = new StringBuilder("[");
		for (int i = 0; i < resultIds.length; i++) {
			results.append(i == 0 ? "" : ",").append("{\"type\":\"user\",\"id\":\"").append(resultIds[i]).append("\"}");
		}
		results.append("]");
		env.putObject("authzen_search_endpoint_response", JsonParser.parseString("{\"results\":" + results + "}").getAsJsonObject());
		if (token == null) {
			env.removeNativeValue("authzen_search_endpoint_request_page_token");
		} else {
			env.putString("authzen_search_endpoint_request_page_token", token);
		}
	}

	@Test
	public void distinctTokens_succeed() {
		pageReturned("t1", "alice");
		cond.execute(env);
		pageReturned("t2", "bob");
		cond.execute(env);
		pageReturned(null, "carol");
		cond.execute(env);
	}

	@Test
	public void repeatedTokenWithSameResults_throws() {
		pageReturned("t1", "alice");
		cond.execute(env);
		pageReturned("t2", "bob");
		cond.execute(env);
		pageReturned("t1", "alice");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("same next_token and the same results as an earlier page"), e.getMessage());
	}

	@Test
	public void sameTokenAndResultsTwiceInARow_throws() {
		pageReturned("t1", "alice");
		cond.execute(env);
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("same next_token and the same results as an earlier page"), e.getMessage());
	}

	@Test
	public void serverSideCursorReturningTheSameTokenWithNewResults_succeeds() {
		pageReturned("cursor-1", "alice");
		cond.execute(env);
		pageReturned("cursor-1", "bob");
		cond.execute(env);
		pageReturned("cursor-1", "carol");
		cond.execute(env);
		pageReturned(null, "dave");
		cond.execute(env);
	}

	@Test
	public void cursorThatComesBackToAnEarlierPage_throws() {
		pageReturned("cursor-1", "alice");
		cond.execute(env);
		pageReturned("cursor-1", "bob");
		cond.execute(env);
		pageReturned("cursor-1", "alice");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("same next_token and the same results as an earlier page"), e.getMessage());
	}

	@Test
	public void emptyToken_endsPaginationAndIsNotRecorded() {
		// An empty next_token signals the last page (Section 8.2), so it is not a token that
		// could come back; two final pages in separate searches must not clash.
		pageReturned("", "alice");
		cond.execute(env);
		pageReturned("", "alice");
		cond.execute(env);
		assertNull(env.getObject("authzen_search_seen_page_tokens"));
	}

	@Test
	public void absentToken_endsPagination() {
		pageReturned(null, "alice");
		cond.execute(env);
		pageReturned(null, "alice");
		cond.execute(env);
		assertNull(env.getObject("authzen_search_seen_page_tokens"));
	}
}
