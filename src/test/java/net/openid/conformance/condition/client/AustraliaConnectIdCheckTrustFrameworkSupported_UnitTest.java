package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AustraliaConnectIdCheckTrustFrameworkSupported_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AustraliaConnectIdCheckTrustFrameworkSupported cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AustraliaConnectIdCheckTrustFrameworkSupported();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_allClaimsSupported() {
		// This matches AustraliaConnectIdCheckTrustFrameworkSupported::ConnectIdTrustFramework
		JsonArray trustFrameworks = JsonParser.parseString(
		"""
		[
		  "au_connectid"
		]
		""").getAsJsonArray();

		env.putObject("server", new JsonObject());
		env.getObject("server").add("trust_frameworks_supported", trustFrameworks);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noClaimsSupported() {
		assertThrows(ConditionError.class, () -> {
			JsonArray trustFrameworks = JsonParser.parseString(
			"""
			[
			  "invalid_claim"
			]
			""").getAsJsonArray();

			env.putObject("server", new JsonObject());
			env.getObject("server").add("trust_frameworks_supported", trustFrameworks);

			cond.execute(env);
		});
	}
}
