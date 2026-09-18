package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemoveMtlsEndpointAliasesFromServerConfiguration_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private RemoveMtlsEndpointAliasesFromServerConfiguration cond;

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new RemoveMtlsEndpointAliasesFromServerConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env = new Environment();
		JsonObject server = JsonParser.parseString("""
			{
				"issuer": "https://example.com",
				"token_endpoint": "https://example.com/token",
				"mtls_endpoint_aliases": {
					"token_endpoint": "https://mtls.example.com/token",
					"userinfo_endpoint": "https://mtls.example.com/userinfo"
				}
			}
			""").getAsJsonObject();
		env.putObject("server", server);
	}

	@Test
	public void removesMtlsEndpointAliasesFromServerConfiguration() {
		cond.execute(env);

		JsonObject server = env.getObject("server");
		assertFalse(server.has("mtls_endpoint_aliases"));
	}

	@Test
	public void leavesOtherServerFieldsUntouched() {
		cond.execute(env);

		JsonObject server = env.getObject("server");
		assertTrue(server.has("issuer"));
		assertTrue(server.has("token_endpoint"));
	}
}
