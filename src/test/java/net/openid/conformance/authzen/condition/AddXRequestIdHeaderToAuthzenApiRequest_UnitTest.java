package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddXRequestIdHeaderToAuthzenApiRequest_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddXRequestIdHeaderToAuthzenApiRequest cond;

	@BeforeEach
	public void setUp() {
		cond = new AddXRequestIdHeaderToAuthzenApiRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private String sentHeader() {
		return OIDFJSON.getString(env.getObject("authzen_api_endpoint_request_headers").get("X-Request-ID"));
	}

	@Test
	public void noHeaders_createsThemWithTheRecordedId() {
		cond.execute(env);
		String recorded = env.getString("authzen_api_endpoint_request_x_request_id");
		assertTrue(recorded.startsWith("conformance-"), recorded);
		assertEquals(recorded, sentHeader());
	}

	@Test
	public void existingHeaders_areKept() {
		env.putObject("authzen_api_endpoint_request_headers", JsonParser.parseString("""
			{ "Authorization": "Bearer key" }""").getAsJsonObject());
		cond.execute(env);
		assertEquals("Bearer key", OIDFJSON.getString(env.getObject("authzen_api_endpoint_request_headers").get("Authorization")));
		assertEquals(env.getString("authzen_api_endpoint_request_x_request_id"), sentHeader());
	}

	@Test
	public void eachRequest_getsAFreshId() {
		// The echo check compares the response with the id of this request, so an id reused
		// from an earlier request could hide a PDP echoing a stale value.
		cond.execute(env);
		String first = sentHeader();
		cond.execute(env);
		String second = sentHeader();
		assertNotEquals(first, second);
		assertEquals(second, env.getString("authzen_api_endpoint_request_x_request_id"));
	}
}
