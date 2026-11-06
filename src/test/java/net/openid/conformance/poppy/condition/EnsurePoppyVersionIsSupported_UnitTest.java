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
public class EnsurePoppyVersionIsSupported_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyVersionIsSupported cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyVersionIsSupported();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String version) {
		env.putString("poppy_version", version);
		cond.execute(env);
	}

	@Test
	public void currentVersionPasses() {
		assertDoesNotThrow(() -> run("0.4"));
	}

	@Test
	public void otherVersionsFail() {
		for (String v : new String[] {"0.3", "1.0", "0.4.1", "v0.4"}) {
			assertThrows(ConditionError.class, () -> run(v), v);
		}
	}
}
