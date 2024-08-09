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
public class CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		JsonObject server = JsonParser.parseString("{"
			+ "\"grant_types_supported\": ["
				+ "\"authorization_code\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missing () {
		JsonObject server = JsonParser.parseString("{"
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_error () {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{"
				+ "\"grant_types_supported\": ["
				+ "\"authorization_codex\""
				+ "]}")
				.getAsJsonObject();
			env.putObject("server", server);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_errorString () {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{"
				+ "\"grant_types_supported\": \"authorization_code\""
				+ "}")
				.getAsJsonObject();
			env.putObject("server", server);
			cond.execute(env);
		});
	}

}
