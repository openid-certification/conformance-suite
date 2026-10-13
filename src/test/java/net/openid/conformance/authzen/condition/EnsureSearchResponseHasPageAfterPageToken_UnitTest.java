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
class EnsureSearchResponseHasPageAfterPageToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureSearchResponseHasPageAfterPageToken cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureSearchResponseHasPageAfterPageToken();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void put(String request, String response) {
		env.putObject("authzen_api_endpoint_request", JsonParser.parseString(request).getAsJsonObject());
		env.putObject("authzen_search_endpoint_response", JsonParser.parseString(response).getAsJsonObject());
	}

	@Test
	public void noToken_responseWithoutPage_succeeds() {
		put("""
			{ "subject": { "type": "user" }, "page": { "limit": 1 } }""",
			"""
			{ "results": [ { "type": "user", "id": "alice" }, { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void token_lastPageWithEmptyNextToken_succeeds() {
		put("""
			{ "subject": { "type": "user" }, "page": { "limit": 1, "token": "cGFnZTI=" } }""",
			"""
			{ "page": { "next_token": "" }, "results": [ { "type": "user", "id": "bob" } ] }""");
		cond.execute(env);
	}

	@Test
	public void token_lastPageWithoutPage_fails() {
		put("""
			{ "subject": { "type": "user" }, "page": { "limit": 1, "token": "cGFnZTI=" } }""",
			"""
			{ "results": [ { "type": "user", "id": "bob" } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no page object"), e.getMessage());
	}

	@Test
	public void token_pageNotAnObject_fails() {
		put("""
			{ "subject": { "type": "user" }, "page": { "token": "cGFnZTI=" } }""",
			"""
			{ "page": "", "results": [] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
