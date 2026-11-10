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
public class SetTokenEndpointToNonMtlsTokenEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private SetTokenEndpointToNonMtlsTokenEndpoint cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new SetTokenEndpointToNonMtlsTokenEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_replacesMtlsAlias() {
		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\": \"https://server.example.com/token\","
			+ "\"mtls_endpoint_aliases\": {"
			+ "\"token_endpoint\": \"https://mtls.example.com/token\""
			+ "}}").getAsJsonObject();
		env.putObject("server", server);
		env.putString("token_endpoint", "https://mtls.example.com/token");

		cond.execute(env);

		assertThat(env.getString("token_endpoint")).isEqualTo("https://server.example.com/token");
	}

	@Test
	public void testEvaluate_noMtlsAliases() {
		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\": \"https://server.example.com/token\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);

		assertThat(env.getString("token_endpoint")).isEqualTo("https://server.example.com/token");
	}

	@Test
	public void testEvaluate_missingTokenEndpoint() {
		JsonObject server = JsonParser.parseString("{"
			+ "\"mtls_endpoint_aliases\": {"
			+ "\"token_endpoint\": \"https://mtls.example.com/token\""
			+ "}}").getAsJsonObject();
		env.putObject("server", server);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
