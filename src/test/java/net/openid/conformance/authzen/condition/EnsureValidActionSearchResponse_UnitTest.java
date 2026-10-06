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
class EnsureValidActionSearchResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureValidActionSearchResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureValidActionSearchResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putResponse(String json) {
		JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("authzen_search_endpoint_response", obj);
	}

	@Test
	public void validActions_succeeds() {
		putResponse("""
			{ "results": [ { "name": "read" }, { "name": "write", "properties": { "soft": true } } ] }""");
		cond.execute(env);
	}

	@Test
	public void actionWithoutName_throws() {
		putResponse("""
			{ "results": [ { "id": "read" } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not contain `name`"));
	}

	@Test
	public void actionWithNonStringName_throws() {
		putResponse("""
			{ "results": [ { "name": [ "read" ] } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("`name` that is not a string"));
	}

	@Test
	public void actionWithEmptyName_succeeds() {
		putResponse("""
			{ "results": [ { "name": "" } ] }""");
		cond.execute(env);
	}

	@Test
	public void actionWithNonObjectProperties_throws() {
		putResponse("""
			{ "results": [ { "name": "read", "properties": [] } ] }""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("`properties` that is not an object"));
	}
}
