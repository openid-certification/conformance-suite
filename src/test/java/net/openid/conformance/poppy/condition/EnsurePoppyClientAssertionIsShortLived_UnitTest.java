package net.openid.conformance.poppy.condition;

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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EnsurePoppyClientAssertionIsShortLived_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyClientAssertionIsShortLived cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyClientAssertionIsShortLived();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(long lifetime) {
		JsonObject claims = new JsonObject();
		claims.addProperty("iat", 1790900000L);
		claims.addProperty("exp", 1790900000L + lifetime);
		JsonObject ca = new JsonObject();
		ca.add("claims", claims);
		env.putObject("client_assertion", ca);
		cond.execute(env);
	}

	@Test
	public void fiveMinutesPasses() {
		assertDoesNotThrow(() -> run(300));
	}

	@Test
	public void justOverFiveMinutesFails() {
		assertThrows(ConditionError.class, () -> run(301));
	}

	@Test
	public void missingExpFails() {
		env.putObject("client_assertion", JsonParser.parseString("{\"claims\":{\"iat\":1}}").getAsJsonObject());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
