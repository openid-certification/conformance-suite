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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsureValidSearchResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureValidSearchResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureValidSearchResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putResponse(String json) {
		JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("authzen_search_endpoint_response", obj);
	}

	@Test
	public void emptyResultsArray_succeeds() {
		putResponse("""
			{
				"results": []
			}""");
		cond.execute(env);
	}

	@Test
	public void resultsWithObjectElements_succeeds() {
		putResponse("""
			{
				"results": [
					{ "name": "view" },
					{ "name": "edit" }
				]
			}""");
		cond.execute(env);
	}

	@Test
	public void contextObject_succeeds() {
		putResponse("""
			{
				"results": [],
				"context": { "k": "v" }
			}""");
		cond.execute(env);
	}

	@Test
	public void missingResults_throws() {
		putResponse("""
			{
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("No results element"));
	}

	@Test
	public void resultsNotArray_throws() {
		putResponse("""
			{
				"results": { "name": "view" }
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not an array"));
	}

	@Test
	public void resultsElementNotObject_throws() {
		putResponse("""
			{
				"results": [ "view" ]
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not an object"));
	}

	@Test
	public void contextNotObject_throws() {
		putResponse("""
			{
				"results": [],
				"context": "ctx"
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("context element"));
	}
}
