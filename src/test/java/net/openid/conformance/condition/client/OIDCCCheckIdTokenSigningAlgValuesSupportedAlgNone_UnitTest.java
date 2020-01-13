package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class OIDCCCheckIdTokenSigningAlgValuesSupportedAlgNone_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private OIDCCCheckIdTokenSigningAlgValuesSupportedAlgNone cond;

	private JsonObject serverObj;

	@Before
	public void setUp() throws Exception {
		cond = new OIDCCCheckIdTokenSigningAlgValuesSupportedAlgNone();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		serverObj = new JsonParser().parse("{" +
				"\"id_token_signing_alg_values_supported\": [" +
					"\"none\"," +
					"\"PS384\"," +
					"\"RS384\"," +
					"\"HS256\"," +
					"\"HS512\"," +
					"\"RS256\"," +
					"\"HS384\"," +
					"\"PS256\"," +
					"\"RS512\"" +
				"]}")
			.getAsJsonObject();
	}

	@Test
	public void testEvaluate_noError() {
		env.putObject("server", serverObj);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIdTokenSigningAlgValuesSupport() {
		serverObj.remove("id_token_signing_alg_values_supported");
		env.putObject("server", serverObj);
		cond.execute(env);

		assertThat(env.getBoolean("id_token_signing_alg_not_supported_flag").equals(true));
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_idTokenSigningAlgValuesSupportIsNotJsonArray() {
		serverObj.addProperty("id_token_signing_alg_values_supported", "is not JsonArray");
		env.putObject("server", serverObj);

		cond.execute(env);

		assertThat(env.getBoolean("id_token_signing_alg_not_supported_flag").equals(true));
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_idTokenSigningAlgValuesSupportNotSupportNoneAlg() {
		serverObj.getAsJsonArray("id_token_signing_alg_values_supported").remove(0);
		env.putObject("server", serverObj);

		cond.execute(env);

		assertThat(env.getBoolean("id_token_signing_alg_not_supported_flag").equals(true));
	}

}
