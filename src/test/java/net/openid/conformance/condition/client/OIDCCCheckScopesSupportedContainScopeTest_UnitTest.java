package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDCCCheckScopesSupportedContainScopeTest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private OIDCCCheckScopesSupportedContainScopeTest cond;

	private JsonObject serverObj;

	private JsonObject clientObj;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new OIDCCCheckScopesSupportedContainScopeTest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		serverObj = JsonParser.parseString(
			"{" +
				"\"scopes_supported\": [" +
					"\"email\"," +
					"\"address\"," +
					"\"openid\"," +
					"\"offline_access\"," +
					"\"phone\"," +
					"\"profile\"," +
					"\"accounts\"," +
					"\"accounts-ob\"," +
					"\"payments\"" +
				"]" +
				"}")
			.getAsJsonObject();

		clientObj = JsonParser.parseString(
			"{\"scope\": \"email profile phone address\"}")
			.getAsJsonObject();
	}

	@Test
	public void testEvaluate_noError() {
		env.putObject("server", serverObj);
		env.putObject("client", clientObj);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingScopesSupported() {
		assertThrows(ConditionError.class, () -> {
			serverObj.remove("scopes_supported");
			env.putObject("server", serverObj);

			env.putObject("client", clientObj);
			cond.execute(env);

			assertThat(env.getBoolean("scopes_not_supported_flag").equals(true));
		});
	}

	@Test
	public void testEvaluate_scopesSupportedIsNotJsonArray() {
		assertThrows(ConditionError.class, () -> {
			serverObj.addProperty("scopes_supported", "is not JsonArray");
			env.putObject("server", serverObj);

			env.putObject("client", clientObj);

			cond.execute(env);

			assertThat(env.getBoolean("scopes_not_supported_flag").equals(true));
		});
	}

	@Test
	public void testEvaluate_scopesSupportedIsNotEnough() {
		assertThrows(ConditionError.class, () -> {
			serverObj.getAsJsonArray("scopes_supported").remove(0);
			env.putObject("server", serverObj);
			env.putObject("client", clientObj);

			cond.execute(env);

			assertThat(env.getBoolean("scopes_not_supported_flag").equals(true));
		});
	}

}
