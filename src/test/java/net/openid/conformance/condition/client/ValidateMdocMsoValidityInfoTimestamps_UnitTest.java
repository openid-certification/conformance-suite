package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdlDataElements;
import net.openid.conformance.util.PhotoIdDataElements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocMsoValidityInfoTimestamps_UnitTest {

	private ValidateMdocMsoValidityInfoTimestamps cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocMsoValidityInfoTimestamps();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private static Tagged tdate(String text) {
		return new Tagged(Tagged.DATE_TIME_STRING, new Tstr(text));
	}

	@Test
	public void testEvaluate_passesForCredentialsTheSuiteIssues() throws Exception {
		for (String docType : new String[] { MdlDataElements.MDL_DOCTYPE,
				PhotoIdDataElements.PHOTO_ID_DOCTYPE }) {
			MdocCredentialTestUtil.putCredential(env,
				MdocCredentialTestUtil.createCredentialBytes(docType));

			assertDoesNotThrow(() -> cond.execute(env), docType);
		}
	}

	@Test
	public void testEvaluate_failsWhenSignedUsesFractionsOfSeconds() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceMsoValidityTimestamp(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			"signed", tdate("2026-08-29T10:00:00.5Z")));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("fractions of seconds"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenValidUntilUsesANumericOffset() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceMsoValidityTimestamp(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			"validUntil", tdate("2027-08-29T10:00:00+00:00")));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenSignedIsAFullDate() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceMsoValidityTimestamp(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			"signed", new Tagged(Tagged.FULL_DATE_STRING, new Tstr("2026-08-29"))));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenValidFromIsMissing() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceMsoValidityTimestamp(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			"validFrom", null));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("validityInfo"), e.getMessage());
	}
}
