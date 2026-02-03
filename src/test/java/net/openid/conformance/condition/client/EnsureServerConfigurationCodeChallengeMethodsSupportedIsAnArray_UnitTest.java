package net.openid.conformance.condition.client;

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
public class EnsureServerConfigurationCodeChallengeMethodsSupportedIsAnArray_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureServerConfigurationCodeChallengeMethodsSupportedIsAnArray cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureServerConfigurationCodeChallengeMethodsSupportedIsAnArray();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_validMethodsArray() {
		JsonObject server= JsonParser.parseString(
		"""
		{
		  "code_challenge_methods_supported": [
		    "S256",
		    "plain"
		  ]
		}
		""").getAsJsonObject();

		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_validEmptyMethodsArray() {
		JsonObject server= JsonParser.parseString(
		"""
		{
		  "code_challenge_methods_supported": [
		  ]
		}
		""").getAsJsonObject();

		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_invalidMethodsArray() {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString(
			"""
			{
			  "code_challenge_methods_supported": [
			    "invalid"
			  ]
			}
			""").getAsJsonObject();

			env.putObject("server", server);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_notAnArray() {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString(
			"""
			{
			  "code_challenge_methods_supported": "plain"
			}
			""").getAsJsonObject();

			env.putObject("server", server);
			cond.execute(env);
		});
	}
}
