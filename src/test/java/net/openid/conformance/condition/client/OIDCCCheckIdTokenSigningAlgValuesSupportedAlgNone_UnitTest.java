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
public class OIDCCCheckIdTokenSigningAlgValuesSupportedAlgNone_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private OIDCCCheckIdTokenSigningAlgValuesSupportedAlgNone cond;

	private JsonObject serverObj;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new OIDCCCheckIdTokenSigningAlgValuesSupportedAlgNone();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		serverObj = JsonParser.parseString("{" +
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

	@Test
	public void testEvaluate_missingIdTokenSigningAlgValuesSupport() {
		assertThrows(ConditionError.class, () -> {
			serverObj.remove("id_token_signing_alg_values_supported");
			env.putObject("server", serverObj);
			cond.execute(env);

			assertThat(env.getBoolean("id_token_signing_alg_not_supported_flag").equals(true));
		});
	}

	@Test
	public void testEvaluate_idTokenSigningAlgValuesSupportIsNotJsonArray() {
		assertThrows(ConditionError.class, () -> {
			serverObj.addProperty("id_token_signing_alg_values_supported", "is not JsonArray");
			env.putObject("server", serverObj);

			cond.execute(env);

			assertThat(env.getBoolean("id_token_signing_alg_not_supported_flag").equals(true));
		});
	}

	@Test
	public void testEvaluate_idTokenSigningAlgValuesSupportNotSupportNoneAlg() {
		assertThrows(ConditionError.class, () -> {
			serverObj.getAsJsonArray("id_token_signing_alg_values_supported").remove(0);
			env.putObject("server", serverObj);

			cond.execute(env);

			assertThat(env.getBoolean("id_token_signing_alg_not_supported_flag").equals(true));
		});
	}

}
