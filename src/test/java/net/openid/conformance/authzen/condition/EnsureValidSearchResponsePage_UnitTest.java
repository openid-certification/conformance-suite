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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsureValidSearchResponsePage_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureValidSearchResponsePage cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureValidSearchResponsePage();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putResponse(String json) {
		JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("authzen_search_endpoint_response", obj);
	}

	@Test
	public void noPageElement_succeedsAndDoesNotSetToken() {
		env.putString("authzen_search_endpoint_request_page_token", "stale");
		putResponse("""
			{
				"results": []
			}""");

		cond.execute(env);

		assertNull(env.getString("authzen_search_endpoint_request_page_token"),
			"Stale token must be cleared when no page element is present");
	}

	@Test
	public void validNextToken_storesTokenInEnv() {
		putResponse("""
			{
				"results": [],
				"page": {
					"next_token": "abc123"
				}
			}""");

		cond.execute(env);

		assertEquals("abc123", env.getString("authzen_search_endpoint_request_page_token"));
	}

	@Test
	public void emptyNextToken_doesNotStoreTokenInEnv() {
		env.putString("authzen_search_endpoint_request_page_token", "stale");
		putResponse("""
			{
				"results": [],
				"page": {
					"next_token": ""
				}
			}""");

		cond.execute(env);

		assertNull(env.getString("authzen_search_endpoint_request_page_token"),
			"Empty next_token must not be stored, and stale token must be cleared first");
	}

	@Test
	public void validPageWithCountAndTotal_succeeds() {
		putResponse("""
			{
				"results": [],
				"page": {
					"next_token": "tok",
					"count": 10,
					"total": 100,
					"properties": { "k": "v" }
				}
			}""");

		cond.execute(env);
	}

	@Test
	public void zeroCountAndTotal_areAllowed() {
		putResponse("""
			{
				"results": [],
				"page": {
					"next_token": "tok",
					"count": 0,
					"total": 0
				}
			}""");

		cond.execute(env);
	}

	@Test
	public void pageNotObject_throws() {
		putResponse("""
			{
				"results": [],
				"page": "not-an-object"
			}""");

		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("page element is not an object"));
	}

	@Test
	public void missingNextToken_throws() {
		putResponse("""
			{
				"results": [],
				"page": {
					"count": 10
				}
			}""");

		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not have next_token"));
	}

	@Test
	public void nextTokenNotString_throws() {
		putResponse("""
			{
				"results": [],
				"page": {
					"next_token": 123
				}
			}""");

		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("next_token element"));
	}

	@Test
	public void countNotNumber_throws() {
		putResponse("""
			{
				"results": [],
				"page": {
					"next_token": "tok",
					"count": "ten"
				}
			}""");

		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not a number"));
	}

	@Test
	public void countNegative_throws() {
		putResponse("""
			{
				"results": [],
				"page": {
					"next_token": "tok",
					"count": -1
				}
			}""");

		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("non-negative number"));
	}

	@Test
	public void totalNegative_throws() {
		putResponse("""
			{
				"results": [],
				"page": {
					"next_token": "tok",
					"total": -5
				}
			}""");

		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("non-negative number"));
	}

	@Test
	public void propertiesNotObject_throws() {
		putResponse("""
			{
				"results": [],
				"page": {
					"next_token": "tok",
					"properties": "no"
				}
			}""");

		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("properties"));
	}
}
