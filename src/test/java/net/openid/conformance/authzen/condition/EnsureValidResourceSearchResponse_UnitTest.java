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
class EnsureValidResourceSearchResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureValidResourceSearchResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureValidResourceSearchResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putRequest(String json) {
		JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("authzen_api_endpoint_request", obj);
	}

	private void putResponse(String json) {
		JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
		env.putObject("authzen_search_endpoint_response", obj);
	}

	@Test
	public void emptyResultsArray_succeeds() {
		putRequest("""
			{
				"resource": {"type":"mail"}
			}""");
		putResponse("""
			{
				"results": []
			}""");
		cond.execute(env);
	}

	@Test
	public void requestWithoutType_throws() {
		putRequest("""
			{
				"resource": {"kind":"mail"}
			}""");

		putResponse("""
			{
				"results": [
					{ "type": "mail", "id":"bob" },
					{ "type": "mail", "id":"alice" }
				]
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no string resource type"));
	}

	@Test
	public void requestWithNonStringType_throws() {
		putRequest("""
			{
				"resource": {"type": 42}
			}""");

		putResponse("""
			{
				"results": [
					{ "type": "mail", "id":"alice" }
				]
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no string resource type"));
	}

	@Test
	public void requestWithNullType_throws() {
		putRequest("""
			{
				"resource": {"type": null}
			}""");

		putResponse("""
			{
				"results": [
					{ "type": "mail", "id":"alice" }
				]
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("no string resource type"));
	}

	@Test
	public void resultsWithRequestedType_succeeds() {
		putRequest("""
			{
				"resource": {"type":"mail"}
			}""");

		putResponse("""
			{
				"results": [
					{ "type": "mail", "id":"bob" },
					{ "type": "mail", "id":"alice" }
				]
			}""");
		cond.execute(env);
	}

	@Test
	public void resultsWithDifferentRequestedType_throws() {
		putRequest("""
			{
				"resource": {"type":"mail"}
			}""");

		putResponse("""
			{
				"results": [
					{ "type": "admin", "id":"bob" },
					{ "type": "user", "id":"alice" }
				]
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not contain the requested resource type"));
	}

	@Test
	public void resultsWithoutRequestedType_throws() {
		putRequest("""
			{
				"resource": {"type":"mail"}
			}""");

		putResponse("""
			{
				"results": [
					{ "id":"bob" },
					{ "type": "user", "id":"alice" }
				]
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not contain `type`"));
	}

	@Test
	public void resultsWithoutId_throws() {
		putRequest("""
			{
				"resource": {"type":"mail"}
			}""");

		putResponse("""
			{
				"results": [
					{ "type":"user", "name":"bob" },
					{ "type": "user", "id":"alice" }
				]
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not contain `id`"));
	}

	@Test
	public void resultsWithNonStringType_throws() {
		putRequest("""
			{
				"resource": {"type":"user"}
			}""");

		putResponse("""
			{
				"results": [
					{ "type": { "name": "user" }, "id":"alice" }
				]
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("`type` that is not a string"));
	}

	@Test
	public void resultsWithNonStringId_throws() {
		putRequest("""
			{
				"resource": {"type":"user"}
			}""");

		putResponse("""
			{
				"results": [
					{ "type": "user", "id": 42 }
				]
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("`id` that is not a string"));
	}

	@Test
	public void resultsWithEmptyId_succeeds() {
		putRequest("""
			{
				"resource": {"type":"user"}
			}""");

		putResponse("""
			{
				"results": [
					{ "type": "user", "id": "" }
				]
			}""");
		cond.execute(env);
	}

	@Test
	public void resultsWithNonObjectProperties_throws() {
		putRequest("""
			{
				"resource": {"type":"user"}
			}""");

		putResponse("""
			{
				"results": [
					{ "type": "user", "id": "alice", "properties": "admin" }
				]
			}""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("`properties` that is not an object"));
	}

	@Test
	public void resultsWithObjectProperties_succeeds() {
		putRequest("""
			{
				"resource": {"type":"user"}
			}""");

		putResponse("""
			{
				"results": [
					{ "type": "user", "id": "alice", "properties": { "role": "admin" } }
				]
			}""");
		cond.execute(env);
	}
}
