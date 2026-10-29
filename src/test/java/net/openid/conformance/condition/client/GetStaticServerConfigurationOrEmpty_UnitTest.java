package net.openid.conformance.condition.client;

import com.google.gson.JsonNull;
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
public class GetStaticServerConfigurationOrEmpty_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private GetStaticServerConfigurationOrEmpty cond;

	@BeforeEach
	public void setUp() {
		cond = new GetStaticServerConfigurationOrEmpty();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_serverPresent() {
		JsonObject server = JsonParser.parseString("{\"request_uri_parameter_supported\":true}").getAsJsonObject();
		JsonObject config = new JsonObject();
		config.add("server", server);
		env.putObject("config", config);

		cond.execute(env);

		assertThat(env.getObject("server")).isEqualTo(server);
	}

	@Test
	public void testEvaluate_serverMissing() {
		env.putObject("config", new JsonObject());

		cond.execute(env);

		assertThat(env.getObject("server")).isEqualTo(new JsonObject());
	}

	@Test
	public void testEvaluate_serverJsonNull() {
		JsonObject config = new JsonObject();
		config.add("server", JsonNull.INSTANCE);
		env.putObject("config", config);

		cond.execute(env);

		assertThat(env.getObject("server")).isEqualTo(new JsonObject());
	}

	@Test
	public void testEvaluate_serverNotObject() {
		JsonObject config = new JsonObject();
		config.addProperty("server", "this is a string");
		env.putObject("config", config);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_serverHasDiscoveryUrl() {
		JsonObject config = new JsonObject();
		config.add("server", JsonParser.parseString("{\"discoveryUrl\":\"https://example.com/.well-known/openid-configuration\"}"));
		env.putObject("config", config);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_configMissing() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
