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
public class ValidateMdocMsoValidUntilAfterValidFrom_UnitTest {

	private ValidateMdocMsoValidUntilAfterValidFrom cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocMsoValidUntilAfterValidFrom();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private static Tagged tdate(Instant instant) {
		return new Tagged(Tagged.DATE_TIME_STRING, new Tstr(instant.truncatedTo(ChronoUnit.SECONDS).toString()));
	}

	private static byte[] withValidityPeriod(Instant validFrom, Instant validUntil) throws Exception {
		byte[] bytes = MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE);
		bytes = MdocCredentialTestUtil.replaceMsoValidityTimestamp(bytes, "validFrom", tdate(validFrom));
		return MdocCredentialTestUtil.replaceMsoValidityTimestamp(bytes, "validUntil", tdate(validUntil));
	}

	@Test
	public void testEvaluate_passesForCredentialTheSuiteIssues() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenValidUntilEqualsValidFrom() throws Exception {
		Instant now = Instant.now();
		MdocCredentialTestUtil.putCredential(env, withValidityPeriod(now, now));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not later than 'validFrom'"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenValidityPeriodIsInvertedWithinClockSkew() throws Exception {
		// Straddles the current time by less than the current-time check's skew allowance, so only
		// a direct comparison of the two timestamps catches it
		Instant now = Instant.now();
		MdocCredentialTestUtil.putCredential(env,
			withValidityPeriod(now.plus(Duration.ofMinutes(2)), now.minus(Duration.ofMinutes(2))));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("not later than 'validFrom'"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenCredentialMissing() {
		env.putString("mdoc_credential_cbor", "");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
