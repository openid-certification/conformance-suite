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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocMsoValidFromNotBeforeSigned_UnitTest {

	private ValidateMdocMsoValidFromNotBeforeSigned cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocMsoValidFromNotBeforeSigned();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private static Tagged tdate(String text) {
		return new Tagged(Tagged.DATE_TIME_STRING, new Tstr(text));
	}

	@Test
	public void testEvaluate_passesForCredentialTheSuiteIssues() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesWhenValidFromIsLaterThanSigned() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceMsoValidityTimestamp(
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE),
			"validFrom", tdate("2099-01-01T00:00:00Z")));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenValidFromPrecedesSigned() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceMsoValidityTimestamp(
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE),
			"validFrom", tdate("2020-01-01T00:00:00Z")));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("earlier than 'signed'"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenCredentialMissing() {
		env.putString("mdoc_credential_cbor", "");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
