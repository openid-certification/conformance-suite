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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VerifyStatusListTokenCwtSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VerifyStatusListTokenCwtSignature cond;

	@BeforeEach
	public void setUp() {
		cond = new VerifyStatusListTokenCwtSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_verifiesSignatureWithX5chainLeaf() throws Exception {
		putToken(StatusListCwtTestFixtures.validStatusListToken());

		assertDoesNotThrow(() -> cond.execute(env));
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
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN,
			StatusListCwtTestFixtures.encode(token));
	}
}
