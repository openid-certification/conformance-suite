package net.openid.conformance.authzen.condition;

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsurePDPJwksConfigured_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePDPJwksConfigured cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePDPJwksConfigured();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putConfig(String json) {
		env.putObject("config", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void jwksConfigured_succeeds() {
		putConfig("{ \"pdp\": { \"jwks\": { \"keys\": [] } } }");
		cond.execute(env);
	}

	@Test
	public void jwksMissing_fails() {
		putConfig("{ \"pdp\": { } }");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("PDP JWK Set"));
	}

	@Test
	public void noPdpSection_fails() {
		putConfig("{ }");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
