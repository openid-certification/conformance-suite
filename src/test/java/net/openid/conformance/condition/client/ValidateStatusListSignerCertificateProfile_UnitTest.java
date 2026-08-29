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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateStatusListSignerCertificateProfile_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateStatusListSignerCertificateProfile cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateStatusListSignerCertificateProfile();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_acceptsTableB9ConformantSignerCertificate() throws Exception {
		putToken(StatusListCwtTestFixtures.validStatusListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsSignerCertificateWithWrongKeyUsage() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListToken(StatusListCwtTestFixtures.DEFAULT_URI,
			StatusListCwtTestFixtures.CertProfile.WRONG_KEY_USAGE));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("key usage"), error.getMessage());
		assertTrue(error.getMessage().contains("digitalSignature"), error.getMessage());
	}

	private void putToken(byte[] token) {
		env.putString(AbstractStatusListCwtCondition.ENV_STATUS_LIST_TOKEN,
			StatusListCwtTestFixtures.encode(token));
	}
}
