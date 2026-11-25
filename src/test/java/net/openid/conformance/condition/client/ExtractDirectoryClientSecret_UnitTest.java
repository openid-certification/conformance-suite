package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ExtractDirectoryClientSecret_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractDirectoryClientSecret cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractDirectoryClientSecret();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("directory_client", JsonParser.parseString("{\"client_id\":\"directory-client\"}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_secretAddedToDirectoryClient() {
		env.putObject("config", JsonParser.parseString("{\"directory\":{\"client_secret\":\"s3cret\"}}").getAsJsonObject());

		cond.execute(env);

		assertThat(env.getString("directory_client", "client_id")).isEqualTo("directory-client");
		assertThat(env.getString("directory_client", "client_secret")).isEqualTo("s3cret");
	}

	@Test
	public void testEvaluate_missingSecret() {
		env.putObject("config", new JsonObject());

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Directory client secret'");
	}

	@Test
	public void testEvaluate_emptySecret() {
		env.putObject("config", JsonParser.parseString("{\"directory\":{\"client_secret\":\"\"}}").getAsJsonObject());

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Directory client secret'");
	}
}
