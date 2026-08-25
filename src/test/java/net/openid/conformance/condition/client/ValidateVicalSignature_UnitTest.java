package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.util.BrainpoolSignatureProvider;
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
public class ValidateVicalSignature_UnitTest {

	private ValidateVicalSignature cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateVicalSignature();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	private void putVical(byte[] signedVical) {
		VicalTestFixtures.putVical(env, signedVical);
	}

	@Test
	public void testEvaluate_passesForValidSignature() {
		VicalTestFixtures.VicalSigner iaca = VicalTestFixtures.generateSigner();
		putVical(VicalTestFixtures.goodSignedVical(List.of(iaca.getCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesForBrainpoolSigner() {
		// as used by the Aptitude-signed Geneva interop VICAL; requires BrainpoolSignatureProvider
		BrainpoolSignatureProvider.ensureInstalled();
		VicalTestFixtures.VicalSigner signer = VicalTestFixtures.generateBrainpoolSigner();
		VicalTestFixtures.VicalSigner iaca = VicalTestFixtures.generateSigner();
		putVical(VicalTestFixtures.goodSignedVical(List.of(iaca.getCert()),
			List.of("org.iso.18013.5.1.mDL"), signer));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsForTamperedSignature() {
		VicalTestFixtures.VicalSigner iaca = VicalTestFixtures.generateSigner();
		byte[] signedVical = VicalTestFixtures.goodSignedVical(List.of(iaca.getCert()));
		// flip a bit in the trailing COSE signature bytes
		signedVical[signedVical.length - 1] ^= 0x01;
		putVical(signedVical);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("signature"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenX5chainMissing() {
		VicalTestFixtures.VicalSigner iaca = VicalTestFixtures.generateSigner();
		byte[] signedVical = VicalTestFixtures.sign(
			VicalTestFixtures.buildVicalMap(List.of(VicalTestFixtures.certificateInfo(iaca.getCert()))),
			VicalTestFixtures.generateSigner(), false);
		putVical(signedVical);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("x5chain"), e.getMessage());
	}
}
