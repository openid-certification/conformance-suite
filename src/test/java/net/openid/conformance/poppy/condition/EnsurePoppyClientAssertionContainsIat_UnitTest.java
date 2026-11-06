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
public class EnsurePoppyClientAssertionContainsIat_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyClientAssertionContainsIat cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyClientAssertionContainsIat();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String claims) {
		env.putObject("client_assertion", JsonParser.parseString("{\"claims\":" + claims + "}").getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void iatPasses() {
		assertDoesNotThrow(() -> run("{\"iat\":1790900000}"));
	}

	@Test
	public void missingIatFails() {
		assertThrows(ConditionError.class, () -> run("{\"exp\":1790900000}"));
	}
}
