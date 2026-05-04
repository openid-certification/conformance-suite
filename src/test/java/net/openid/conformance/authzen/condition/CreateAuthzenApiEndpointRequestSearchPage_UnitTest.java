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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CreateAuthzenApiEndpointRequestSearchPage_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		env.putObject("authzen_api_endpoint_request", new JsonObject());
	}

	private CreateAuthzenApiEndpointRequestSearchPage cond(String pageJson) {
		JsonObject pageParam = pageJson == null ? null : JsonParser.parseString(pageJson).getAsJsonObject();
		CreateAuthzenApiEndpointRequestSearchPage c = new CreateAuthzenApiEndpointRequestSearchPage(pageParam);
		c.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		return c;
	}

	private JsonObject pageInRequest() {
		return env.getObject("authzen_api_endpoint_request").getAsJsonObject("page");
	}

	@Test
	public void envTokenSet_andNoTokenInRequest_copiesEnvTokenIntoRequest() {
		env.putString("authzen_search_endpoint_request_page_token", "ENV-TOKEN");

		cond("""
			{
				"limit": 10
			}""").execute(env);

		JsonObject page = pageInRequest();
		assertTrue(page.has("token"));
		assertEquals("ENV-TOKEN", OIDFJSON.getString(page.get("token")));
		assertEquals(10, OIDFJSON.getInt(page.get("limit")));
	}

	@Test
	public void envTokenSet_butRequestAlreadyHasToken_doesNotOverride() {
		env.putString("authzen_search_endpoint_request_page_token", "ENV-TOKEN");

		cond("""
			{
				"token": "REQ-TOKEN"
			}""").execute(env);

		assertEquals("REQ-TOKEN", OIDFJSON.getString(pageInRequest().get("token")));
	}

	@Test
	public void noEnvToken_andNoTokenInRequest_omitsToken() {
		cond("""
			{
				"limit": 5
			}""").execute(env);

		assertFalse(pageInRequest().has("token"),
			"token should not be added when neither env nor request supplies one");
	}

	@Test
	public void emptyEnvToken_doesNotAddToken() {
		env.putString("authzen_search_endpoint_request_page_token", "");

		cond("""
			{
			}""").execute(env);

		assertFalse(pageInRequest().has("token"));
	}

	@Test
	public void copiesOptionalProperties() {
		cond("""
			{
				"token": "T",
				"limit": 50,
				"properties": { "k": "v" }
			}""").execute(env);

		JsonObject page = pageInRequest();
		assertEquals("T", OIDFJSON.getString(page.get("token")));
		assertEquals(50, OIDFJSON.getInt(page.get("limit")));
		assertTrue(page.has("properties"));
	}
}
