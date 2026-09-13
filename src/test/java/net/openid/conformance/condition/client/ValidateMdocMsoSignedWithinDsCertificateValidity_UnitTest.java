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
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocMsoSignedWithinDsCertificateValidity_UnitTest {

	private ValidateMdocMsoSignedWithinDsCertificateValidity cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocMsoSignedWithinDsCertificateValidity();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private static Tagged tdate(Instant instant) {
		return new Tagged(Tagged.DATE_TIME_STRING, new Tstr(instant.toString()));
	}

	@Test
	public void testEvaluate_passesWhenSignedIsInsideCertificateValidity() throws Exception {
		Instant now = Instant.now();
		MdocCredentialTestUtil.putCredential(env, MdocDsCertificateTestFixtures.credentialWithDsCertValidity(
			Date.from(now.minus(Duration.ofDays(2))), Date.from(now.plus(Duration.ofDays(1)))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenSignedPrecedesCertificateNotBefore() throws Exception {
		Instant now = Instant.now();
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceMsoValidityTimestamp(
			MdocDsCertificateTestFixtures.credentialWithDsCertValidity(
				Date.from(now.minus(Duration.ofDays(2))), Date.from(now.plus(Duration.ofDays(1)))),
			"signed", tdate(now.minus(Duration.ofDays(3)))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("outside the validity period"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenSignedFollowsCertificateNotAfter() throws Exception {
		Instant now = Instant.now();
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceMsoValidityTimestamp(
			MdocDsCertificateTestFixtures.credentialWithDsCertValidity(
				Date.from(now.minus(Duration.ofDays(2))), Date.from(now.plus(Duration.ofDays(1)))),
			"signed", tdate(now.plus(Duration.ofDays(2)))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenCredentialMissing() {
		env.putString("mdoc_credential_cbor", "");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
