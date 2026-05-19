package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class SetClient2IdToIncludeClientIdScheme_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private SetClient2IdToIncludeClientIdScheme cond;

	private JsonObject config;

	@BeforeEach
	public void setUp() {
		cond = new SetClient2IdToIncludeClientIdScheme();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		config = JsonParser.parseString("""
			{
				"client2": {
					"client_id": "wallet-second"
				}
			}
			""").getAsJsonObject();
	}

	@Test
	public void testEvaluate_success() {
		env.putObject("config", config);
		env.putString("client_id_scheme", "pre_registered");

		cond.execute(env);

		assertThat(env.getString("client2_id")).isEqualTo("pre_registered:wallet-second");
		assertThat(env.getString("orig_client2_id")).isEqualTo("wallet-second");
	}

	@Test
	public void testEvaluate_missingClient2Id() {
		env.putObject("config", JsonParser.parseString("""
			{
				"client2": {}
			}
			""").getAsJsonObject());
		env.putString("client_id_scheme", "pre_registered");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
