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
import org.multipaz.documenttype.knowntypes.DrivingLicense;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocMsoValidityPeriodIsCurrent_UnitTest {

	private ValidateMdocMsoValidityPeriodIsCurrent cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocMsoValidityPeriodIsCurrent();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private static Tagged tdate(Instant instant) {
		return new Tagged(Tagged.DATE_TIME_STRING, new Tstr(instant.truncatedTo(ChronoUnit.SECONDS).toString()));
	}

	private static byte[] withTimestamp(String name, Instant value) throws Exception {
		return MdocCredentialTestUtil.replaceMsoValidityTimestamp(
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE), name, tdate(value));
	}

	@Test
	public void testEvaluate_passesForCredentialTheSuiteIssues() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesWhenValidFromIsWithinAllowedSkew() throws Exception {
		MdocCredentialTestUtil.putCredential(env, withTimestamp("validFrom", Instant.now().plus(Duration.ofMinutes(2))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenValidFromIsInTheFuture() throws Exception {
		MdocCredentialTestUtil.putCredential(env, withTimestamp("validFrom", Instant.now().plus(Duration.ofDays(1))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not yet valid"), e.getMessage());
	}

	@Test
	public void testEvaluate_passesWhenValidUntilIsWithinAllowedSkew() throws Exception {
		MdocCredentialTestUtil.putCredential(env, withTimestamp("validUntil", Instant.now().minus(Duration.ofMinutes(2))));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenValidUntilIsInThePast() throws Exception {
		MdocCredentialTestUtil.putCredential(env, withTimestamp("validUntil", Instant.now().minus(Duration.ofDays(1))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("expired"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenCredentialMissing() {
		env.putString("mdoc_credential_cbor", "");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
