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
public class ValidateIdentifierListSignerCertificateProfile_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateIdentifierListSignerCertificateProfile cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateIdentifierListSignerCertificateProfile();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_acceptsATableB9ConformantSigner() throws Exception {
		putToken(IdentifierListCwtTestFixtures.validIdentifierListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenNoTokenIsStored() {
		// the condition reads the identifier list token, not the status list token, so a stored
		// status list token must not make it pass
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN, "irrelevant");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void putToken(byte[] token) {
		env.putString(AbstractIdentifierListCwtCondition.ENV_IDENTIFIER_LIST_TOKEN,
			IdentifierListCwtTestFixtures.encode(token));
	}
}
