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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateRicalSignature_UnitTest {

	private ValidateRicalSignature cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private RicalTestFixtures.ReaderPki pki;

	@BeforeEach
	public void setUp() {
		cond = new ValidateRicalSignature();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
		pki = RicalTestFixtures.generateReaderPki();
	}

	@Test
	public void testEvaluate_passesForGoodSignature() {
		RicalTestFixtures.putRical(env, RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenPayloadTampered() {
		byte[] rical = RicalTestFixtures.goodSignedRical(List.of(pki.getCaCert()));
		// flip a bit near the end of the payload so the COSE signature no longer verifies
		rical[rical.length / 2] ^= 0x01;
		RicalTestFixtures.putRical(env, rical);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenX5chainInUnprotectedHeader() {
		byte[] rical = RicalTestFixtures.sign(
			RicalTestFixtures.buildRicalMap(List.of(RicalTestFixtures.certificateInfo(pki.getCaCert()))),
			RicalTestFixtures.generateSigner(), false, true);
		RicalTestFixtures.putRical(env, rical);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("protected"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenX5chainMissing() {
		byte[] rical = RicalTestFixtures.sign(
			RicalTestFixtures.buildRicalMap(List.of(RicalTestFixtures.certificateInfo(pki.getCaCert()))),
			RicalTestFixtures.generateSigner(), true, false);
		RicalTestFixtures.putRical(env, rical);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("x5chain"), e.getMessage());
	}
}
