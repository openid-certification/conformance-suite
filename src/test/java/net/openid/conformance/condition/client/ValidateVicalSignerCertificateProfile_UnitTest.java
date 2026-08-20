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
public class ValidateVicalSignerCertificateProfile_UnitTest {

	private ValidateVicalSignerCertificateProfile cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateVicalSignerCertificateProfile();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	private void putVicalSignedBy(VicalTestFixtures.VicalSigner signer) {
		VicalTestFixtures.VicalSigner iaca = VicalTestFixtures.generateSigner();
		byte[] signedVical = VicalTestFixtures.sign(
			VicalTestFixtures.buildVicalMap(List.of(VicalTestFixtures.certificateInfo(iaca.getCert()))),
			signer);
		VicalTestFixtures.putVical(env, signedVical);
	}

	@Test
	public void testEvaluate_passesForCompliantSignerCert() {
		putVicalSignedBy(VicalTestFixtures.generateSigner());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenEkuMissing() {
		putVicalSignedBy(VicalTestFixtures.generateSigner(false));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("key usage"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenSignerCertExpired() {
		long now = System.currentTimeMillis();
		VicalTestFixtures.VicalSigner expired = VicalTestFixtures.generateSigner(true,
			kotlin.time.Instant.Companion.fromEpochMilliseconds(now - 400L * 24 * 3600 * 1000),
			kotlin.time.Instant.Companion.fromEpochMilliseconds(now - 30L * 24 * 3600 * 1000));
		putVicalSignedBy(expired);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("validity"), e.getMessage());
	}
}
