package net.openid.conformance.condition.as.par;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

public class AddPushedAuthorizationRequestEndpointToServerConfig_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddPushedAuthorizationRequestEndpointToServerConfig cond;

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new AddPushedAuthorizationRequestEndpointToServerConfig();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env = new Environment();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> capturedLogArgs() {
		ArgumentCaptor<Map<String, Object>> args = ArgumentCaptor.forClass(Map.class);
		verify(eventLog).log(anyString(), args.capture());
		return args.getValue();
	}

	@Test
	public void logMentionsTheMtlsAliasedEndpointWhenAliasesArePresent() {
		// Regression test: this log entry previously only reported the root "endpoint" value even
		// when an mTLS alias was also added, which reads as if the alias were never added at all.
		JsonObject server = JsonParser.parseString("""
			{
				"token_endpoint": "https://as.example.com/token",
				"mtls_endpoint_aliases": {
					"token_endpoint": "https://mtls.as.example.com/token"
				}
			}
			""").getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);

		assertThat(capturedLogArgs()).containsEntry("mtls_endpoint", "https://mtls.as.example.com/par");
	}

	@Test
	public void logDoesNotClaimAnMtlsEndpointWhenAliasesAreAbsent() {
		JsonObject server = JsonParser.parseString("""
			{
				"token_endpoint": "https://as.example.com/token"
			}
			""").getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);

		assertThat(capturedLogArgs()).doesNotContainKey("mtls_endpoint");
	}
}
