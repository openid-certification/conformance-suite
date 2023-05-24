package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AustraliaConnectIdCheckClaimsSupported_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdCheckClaimsSupported cond;

	@Before
	public void setUp() throws Exception {
		cond = new AustraliaConnectIdCheckClaimsSupported();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_allClaimsSupported() {
		// This matches AustraliaConnectIdCheckClaimsSupported::ConnectIdMandatoryToSupportClaims
		JsonObject server = JsonParser.parseString("{"
			+ "\"claims_supported\": ["
				+ "\"name\","
				+ "\"given_name\","
				+ "\"family_name\","
				+ "\"email\","
				+ "\"birthdate\","
				+ "\"phone_number\","
				+ "\"address\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_singleClaimSupported() {
		JsonObject server = JsonParser.parseString("{"
			+ "\"claims_supported\": ["
				+ "\"address\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_noClaimsSupported() {
		JsonObject server = JsonParser.parseString("{"
			+ "\"claims_supported\": ["
				+ "\"invalid_claim\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}
}
