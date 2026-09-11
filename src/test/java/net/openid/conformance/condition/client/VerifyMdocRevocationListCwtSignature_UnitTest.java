package net.openid.conformance.condition.client;

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
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.Tagged;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.Algorithm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VerifyMdocRevocationListCwtSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VerifyMdocRevocationListCwtSignature cond;

	@BeforeEach
	public void setUp() {
		cond = new VerifyMdocRevocationListCwtSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_verifiesSignatureWithX5chainLeaf() throws Exception {
		putToken(StatusListCwtTestFixtures.validStatusListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsAnAlgorithmNotPairedWithTheSignersCurve() throws Exception {
		// ES384 over the fixture's P-256 key verifies cryptographically, but ISO/IEC 18013-5
		// 12.3.6.3 pairs ES384 with P-384 and the 320/384-bit Brainpool curves only
		putToken(StatusListCwtTestFixtures.statusListTokenWithAlgorithm(Algorithm.ES384));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("ES384"), error.getMessage());
		assertTrue(error.getMessage().contains("P-256"), error.getMessage());
	}

	@Test
	public void testEvaluate_rejectsTamperedPayload() throws Exception {
		byte[] token = StatusListCwtTestFixtures.validStatusListToken();
		CoseSign1 original = CoseSign1.Companion.fromDataItem(
			((Tagged) Cbor.INSTANCE.decode(token)).getTaggedItem());

		byte[] tamperedPayload = original.getPayload().clone();
		tamperedPayload[tamperedPayload.length - 1] ^= 0x01;

		CoseSign1 tampered = new CoseSign1(original.getProtectedHeaders(),
			original.getUnprotectedHeaders(), original.getSignature(), tamperedPayload);
		putToken(Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, tampered.toDataItem())));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsTokenWithoutProtectedX5chain() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListTokenWithX5chainInUnprotectedHeader());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putToken(byte[] token) {
		env.putString(AbstractRevocationListCwtCondition.ENV_TOKEN, StatusListCwtTestFixtures.encode(token));
	}
}
