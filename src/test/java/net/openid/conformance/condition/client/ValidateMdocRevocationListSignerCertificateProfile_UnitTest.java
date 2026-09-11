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
import org.multipaz.crypto.X509KeyUsage;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocRevocationListSignerCertificateProfile_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateMdocRevocationListSignerCertificateProfile cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocRevocationListSignerCertificateProfile();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_acceptsTableB9ConformantSignerCertificate() throws Exception {
		putToken(StatusListCwtTestFixtures.validStatusListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_acceptsTableB9ConformantIdentifierListSignerCertificate() throws Exception {
		putToken(IdentifierListCwtTestFixtures.validIdentifierListToken());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsSignerCertificateWithWrongKeyUsage() throws Exception {
		putToken(StatusListCwtTestFixtures.statusListTokenWithSignerKeyUsage(
			Set.of(X509KeyUsage.KEY_CERT_SIGN, X509KeyUsage.CRL_SIGN)));

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("key usage"), error.getMessage());
		assertTrue(error.getMessage().contains("digitalSignature"), error.getMessage());
	}

	private void putToken(byte[] token) {
		env.putString(AbstractRevocationListCwtCondition.ENV_TOKEN, StatusListCwtTestFixtures.encode(token));
	}
}
