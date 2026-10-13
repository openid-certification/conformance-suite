package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class AddAuthzenSearchPageTokenToRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddAuthzenSearchPageTokenToRequest cond;

	@BeforeEach
	public void setUp() {
		cond = new AddAuthzenSearchPageTokenToRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putRequest(String json) {
		env.putObject("authzen_api_endpoint_request", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void token_isAddedToExistingPage_keepingOtherMembers() {
		putRequest("""
			{ "subject": { "type": "user" }, "page": { "limit": 10, "customAttr": "ignored" } }""");
		env.putString("authzen_search_endpoint_request_page_token", "cGFnZTI=");
		cond.execute(env);
		JsonObject page = env.getObject("authzen_api_endpoint_request").getAsJsonObject("page");
		assertEquals("cGFnZTI=", OIDFJSON.getString(page.get("token")));
		assertEquals(10, OIDFJSON.getInt(page.get("limit")));
		assertEquals("ignored", OIDFJSON.getString(page.get("customAttr")));
	}

	@Test
	public void token_createsPageWhenAbsent() {
		putRequest("""
			{ "subject": { "type": "user" }, "foo": "bar" }""");
		env.putString("authzen_search_endpoint_request_page_token", "cGFnZTI=");
		cond.execute(env);
		assertEquals("cGFnZTI=", OIDFJSON.getString(env.getObject("authzen_api_endpoint_request").getAsJsonObject("page").get("token")));
	}

	@Test
	public void noToken_leavesRequestUnchanged() {
		putRequest("""
			{ "subject": { "type": "user" } }""");
		cond.execute(env);
		assertFalse(env.getObject("authzen_api_endpoint_request").has("page"));
	}
}
