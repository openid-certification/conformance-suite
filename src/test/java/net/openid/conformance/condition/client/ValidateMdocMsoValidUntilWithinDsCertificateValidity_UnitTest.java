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

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocMsoValidUntilWithinDsCertificateValidity_UnitTest {

	private ValidateMdocMsoValidUntilWithinDsCertificateValidity cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocMsoValidUntilWithinDsCertificateValidity();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	public void testEvaluate_passesWhenCertificateOutlivesTheMso() throws Exception {
		// the suite's credentials are valid for a year
		Instant now = Instant.now();
		MdocCredentialTestUtil.putCredential(env, MdocDsCertificateTestFixtures.credentialWithDsCertValidity(
			Date.from(now.minus(Duration.ofDays(1))), Date.from(now.plus(Duration.ofDays(400)))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenMsoOutlivesTheCertificate() throws Exception {
		Instant now = Instant.now();
		MdocCredentialTestUtil.putCredential(env, MdocDsCertificateTestFixtures.credentialWithDsCertValidity(
			Date.from(now.minus(Duration.ofDays(1))), Date.from(now.plus(Duration.ofDays(30)))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("later than the notAfter"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenCredentialMissing() {
		env.putString("mdoc_credential_cbor", "");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
