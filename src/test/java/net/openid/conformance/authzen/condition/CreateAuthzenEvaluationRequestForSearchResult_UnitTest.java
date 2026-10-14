package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class CreateAuthzenEvaluationRequestForSearchResult_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	private void run(AbstractCondition cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	@Test
	public void evaluationRequest_replacesTheSearchedEntityAndDropsPage() {
		JsonObject search = json("""
			{ "subject": { "type": "user" }, "action": { "name": "read" },
			  "resource": { "type": "record", "id": "record-1" },
			  "context": { "ip": "192.168.1.1" }, "page": { "token": "abc" } }""");
		run(new CreateAuthzenEvaluationRequestForSearchResult(search, "subject", json("""
			{ "type": "user", "id": "bob" }""")));
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		assertEquals("bob", OIDFJSON.getString(request.getAsJsonObject("subject").get("id")));
		assertEquals("record-1", OIDFJSON.getString(request.getAsJsonObject("resource").get("id")));
		assertEquals("192.168.1.1", OIDFJSON.getString(request.getAsJsonObject("context").get("ip")));
		assertFalse(request.has("page"));
	}

	@Test
	public void evaluationRequest_addsTheActionForAnActionSearch() {
		JsonObject search = json("""
			{ "subject": { "type": "user", "id": "alice" }, "resource": { "type": "record", "id": "record-1" } }""");
		run(new CreateAuthzenEvaluationRequestForSearchResult(search, "action", json("""
			{ "name": "write" }""")));
		assertEquals("write", OIDFJSON.getString(env.getObject("authzen_api_endpoint_request").getAsJsonObject("action").get("name")));
	}

	@Test
	public void evaluationRequest_withoutContext_hasNoContext() {
		JsonObject search = json("""
			{ "subject": { "type": "user" }, "action": { "name": "read" },
			  "resource": { "type": "record", "id": "record-1" } }""");
		run(new CreateAuthzenEvaluationRequestForSearchResult(search, "subject", json("""
			{ "type": "user", "id": "alice" }""")));
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		assertFalse(request.has("context"));
		assertEquals(3, request.size());
	}

	@Test
	public void evaluationRequest_doesNotShareObjectsWithTheSearchRequestOrResult() {
		// The module reuses the search request and result for each result it evaluates, so the
		// request built here must be a copy.
		JsonObject search = json("""
			{ "subject": { "type": "user", "id": "alice" }, "resource": { "type": "record", "id": "record-1" },
			  "context": { "ip": "192.168.1.1" } }""");
		JsonObject result = json("""
			{ "name": "read" }""");
		run(new CreateAuthzenEvaluationRequestForSearchResult(search, "action", result));
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		request.getAsJsonObject("context").addProperty("ip", "10.0.0.1");
		request.getAsJsonObject("action").addProperty("name", "write");
		assertEquals("192.168.1.1", OIDFJSON.getString(search.getAsJsonObject("context").get("ip")));
		assertEquals("read", OIDFJSON.getString(result.get("name")));
	}

	@Test
	public void evaluationRequest_dropsTheSearchRequestHeaders() {
		// The last search page's headers (its authentication, or an X-Request-ID) must not be
		// sent with the evaluation call; the module adds the authentication again.
		env.putObject("authzen_api_endpoint_request_headers", json("""
			{ "Authorization": "Bearer search-key", "X-Request-ID": "search-page-2" }"""));
		JsonObject search = json("""
			{ "subject": { "type": "user", "id": "alice" }, "resource": { "type": "record", "id": "record-1" } }""");
		run(new CreateAuthzenEvaluationRequestForSearchResult(search, "action", json("""
			{ "name": "read" }""")));
		assertNull(env.getObject("authzen_api_endpoint_request_headers"));
	}
}
