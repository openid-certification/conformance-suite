package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ExtractInitialAccessTokenFromStoredConfig_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractInitialAccessTokenFromStoredConfig cond;

	/**
	 * @throws Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExtractInitialAccessTokenFromStoredConfig();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noInitialAccessTokenInConfig() {
		env.putObject("original_client_config", new JsonObject());

		cond.execute(env);

		assertThat(env.getObject("initial_access_token")).isNull();
	}

	/**
	 * Test method for {@link ExtractInitialAccessTokenFromStoredConfig#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_InitialAccessTokenInConfig() {
		String requestAccessToken = "mF_9.B5f-4.1JqM";

		JsonObject config = JsonParser.parseString("{" +
			"\"initial_access_token\":\"" + requestAccessToken + "\"" +
			"}").getAsJsonObject();
		env.putObject("original_client_config", config);

		cond.execute(env);

		assertThat(env.getString("initial_access_token")).isNotNull();
		assertThat(env.getString("initial_access_token")).isEqualTo(requestAccessToken);
	}
}
