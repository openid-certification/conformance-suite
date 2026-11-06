package net.openid.conformance.poppy.condition;

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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsurePoppyAuthNotDisabled_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyAuthNotDisabled cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyAuthNotDisabled();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void absentPasses() {
		env.putObject("poppy_json", PoppyTestData.json("{}"));
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void falsePasses() {
		env.putObject("poppy_json", PoppyTestData.json("{\"auth\": {\"disabled\": false}}"));
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void trueFails() {
		env.putObject("poppy_json", PoppyTestData.json("{\"auth\": {\"disabled\": true}}"));
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("Direct Sign-In").contains("3.2").contains("enable sign-in");
	}
}
