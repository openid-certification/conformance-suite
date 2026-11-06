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
public class EnsurePoppyRootWellKnownAlias_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyRootWellKnownAlias cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyRootWellKnownAlias();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void rootAliasPasses() {
		env.putObject("config", PoppyTestData.json("{\"alias\": \"root-well-known\"}"));
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void otherAliasFails() {
		env.putObject("config", PoppyTestData.json("{\"alias\": \"other\"}"));
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("alias").contains("root-well-known");
	}

	@Test
	public void missingAliasFails() {
		env.putObject("config", new com.google.gson.JsonObject());
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertThat(e.getMessage()).contains("alias");
	}
}
