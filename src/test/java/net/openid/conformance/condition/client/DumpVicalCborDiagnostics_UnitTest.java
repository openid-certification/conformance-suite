package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class DumpVicalCborDiagnostics_UnitTest {

	private DumpVicalCborDiagnostics cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new DumpVicalCborDiagnostics();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void testEvaluate_dumpsValidVical() {
		VicalTestFixtures.VicalSigner iaca = VicalTestFixtures.generateSigner();
		VicalTestFixtures.putVical(env, VicalTestFixtures.goodSignedVical(List.of(iaca.getCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsOnUndecodableBytes() {
		VicalTestFixtures.putVical(env, new byte[] { (byte) 0xff, 0x01, 0x02 });

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
