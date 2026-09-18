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

public class SetBackchannelAuthenticationEndpointToMtlsBackchannelAuthenticationEndpoint_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private SetBackchannelAuthenticationEndpointToMtlsBackchannelAuthenticationEndpoint cond;

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new SetBackchannelAuthenticationEndpointToMtlsBackchannelAuthenticationEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env = new Environment();
	}

	@Test
	public void setsBackchannelAuthenticationEndpointToTheMtlsAliasValue() {
		JsonObject server = JsonParser.parseString("""
			{
				"backchannel_authentication_endpoint": "https://example.com/backchannel",
				"mtls_endpoint_aliases": {
					"backchannel_authentication_endpoint": "https://mtls.example.com/backchannel"
				}
			}
			""").getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);

		assertEquals("https://mtls.example.com/backchannel", env.getString("server", "backchannel_authentication_endpoint"));
	}

	@Test
	public void throwsWhenMtlsEndpointAliasesIsMissing() {
		JsonObject server = JsonParser.parseString("""
			{
				"backchannel_authentication_endpoint": "https://example.com/backchannel"
			}
			""").getAsJsonObject();
		env.putObject("server", server);

		assertThrows(RuntimeException.class, () -> cond.execute(env));
	}

	@Test
	public void throwsWhenBackchannelAuthenticationEndpointAliasIsMissing() {
		JsonObject server = JsonParser.parseString("""
			{
				"backchannel_authentication_endpoint": "https://example.com/backchannel",
				"mtls_endpoint_aliases": {
					"token_endpoint": "https://mtls.example.com/token"
				}
			}
			""").getAsJsonObject();
		env.putObject("server", server);

		assertThrows(RuntimeException.class, () -> cond.execute(env));
	}
}
