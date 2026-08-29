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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VerifyIdentifierListTokenCwtSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VerifyIdentifierListTokenCwtSignature cond;

	@BeforeEach
	public void setUp() {
		cond = new VerifyIdentifierListTokenCwtSignature();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_verifiesAgainstTheX5chainLeaf() throws Exception {
		putToken(IdentifierListCwtTestFixtures.validIdentifierListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsATamperedPayload() throws Exception {
		byte[] token = IdentifierListCwtTestFixtures.validIdentifierListToken();
		// flip a byte in the middle of the encoded COSE_Sign1; the payload and signature no
		// longer agree whichever field it lands in
		token[token.length / 2] ^= 0x40;
		putToken(token);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putToken(byte[] token) {
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN,
			IdentifierListCwtTestFixtures.encode(token));
	}
}
