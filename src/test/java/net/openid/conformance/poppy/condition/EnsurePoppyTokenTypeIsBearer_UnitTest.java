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
public class EnsurePoppyTokenTypeIsBearer_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyTokenTypeIsBearer cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyTokenTypeIsBearer();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String json) {
		env.putObject("token_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void bearerPasses() {
		assertDoesNotThrow(() -> run("{\"token_type\":\"Bearer\"}"));
		assertDoesNotThrow(() -> run("{\"token_type\":\"bearer\"}"));
	}

	@Test
	public void dpopFails() {
		ConditionError e = assertThrows(ConditionError.class, () -> run("{\"token_type\":\"DPoP\"}"));
		assertTrue(e.getMessage().contains("did not send a DPoP proof"));
	}

	@Test
	public void missingFails() {
		assertThrows(ConditionError.class, () -> run("{}"));
	}
}
