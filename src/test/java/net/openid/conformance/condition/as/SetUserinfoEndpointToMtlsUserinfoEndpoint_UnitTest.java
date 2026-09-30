package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SetUserinfoEndpointToMtlsUserinfoEndpoint_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private SetUserinfoEndpointToMtlsUserinfoEndpoint cond;

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new SetUserinfoEndpointToMtlsUserinfoEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env = new Environment();
	}

	@Test
	public void setsUserinfoEndpointToTheMtlsAliasValue() {
		JsonObject server = JsonParser.parseString("""
			{
				"userinfo_endpoint": "https://example.com/userinfo",
				"mtls_endpoint_aliases": {
					"userinfo_endpoint": "https://mtls.example.com/userinfo"
				}
			}
			""").getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);

		assertEquals("https://mtls.example.com/userinfo", env.getString("server", "userinfo_endpoint"));
	}

	@Test
	public void throwsWhenMtlsEndpointAliasesIsMissing() {
		JsonObject server = JsonParser.parseString("""
			{
				"userinfo_endpoint": "https://example.com/userinfo"
			}
			""").getAsJsonObject();
		env.putObject("server", server);

		assertThrows(RuntimeException.class, () -> cond.execute(env));
	}

	@Test
	public void throwsWhenUserinfoEndpointAliasIsMissing() {
		JsonObject server = JsonParser.parseString("""
			{
				"userinfo_endpoint": "https://example.com/userinfo",
				"mtls_endpoint_aliases": {
					"token_endpoint": "https://mtls.example.com/token"
				}
			}
			""").getAsJsonObject();
		env.putObject("server", server);

		assertThrows(RuntimeException.class, () -> cond.execute(env));
	}
}
