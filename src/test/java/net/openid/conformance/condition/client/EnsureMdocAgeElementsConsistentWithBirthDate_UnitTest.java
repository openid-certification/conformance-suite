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
import org.multipaz.cbor.DataItemExtensionsKt;
import org.multipaz.cbor.Simple;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureMdocAgeElementsConsistentWithBirthDate_UnitTest {

	private EnsureMdocAgeOverElementsConsistentWithBirthDate ageOver;

	private EnsureMdocAgeInYearsConsistentWithBirthDate ageInYears;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		ageOver = new EnsureMdocAgeOverElementsConsistentWithBirthDate();
		ageOver.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		ageInYears = new EnsureMdocAgeInYearsConsistentWithBirthDate();
		ageInYears.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	private byte[] photoIdCredential() throws Exception {
		return MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE);
	}

	@Test
	public void testEvaluate_passesForCredentialsTheSuiteIssues() throws Exception {
		for (String docType : new String[] { PhotoIdDataElements.PHOTO_ID_DOCTYPE,
				MdlDataElements.MDL_DOCTYPE, "eu.europa.ec.eudi.pid.1" }) {
			MdocCredentialTestUtil.putCredential(env,
				MdocCredentialTestUtil.createCredentialBytes(docType));

			assertDoesNotThrow(() -> ageOver.execute(env), docType);
			assertDoesNotThrow(() -> ageInYears.execute(env), docType);
		}
	}

	@Test
	public void testEvaluate_failsWhenAgeInYearsDisagreesWithBirthDate() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceElementValue(
			photoIdCredential(), PhotoIdDataElements.ISO_23220_2_NAMESPACE,
			"age_in_years", DataItemExtensionsKt.toDataItem(99)));

		ConditionError e = assertThrows(ConditionError.class, () -> ageInYears.execute(env));
		assertTrue(e.getMessage().contains("birth_date"), e.getMessage());
		assertDoesNotThrow(() -> ageOver.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenAgeBirthYearDisagreesWithBirthDate() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceElementValue(
			photoIdCredential(), PhotoIdDataElements.ISO_23220_2_NAMESPACE,
			"age_birth_year", DataItemExtensionsKt.toDataItem(1990)));

		assertThrows(ConditionError.class, () -> ageInYears.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenAgeOverIsFalseForAnOlderHolder() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.replaceElementValue(
			photoIdCredential(), PhotoIdDataElements.ISO_23220_2_NAMESPACE,
			"age_over_18", Simple.Companion.getFALSE()));

		ConditionError e = assertThrows(ConditionError.class, () -> ageOver.execute(env));
		assertTrue(e.getMessage().contains("age_over_NN"), e.getMessage());
		assertDoesNotThrow(() -> ageInYears.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenAgeOverIsTrueForAYoungerHolder() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.addElement(
			photoIdCredential(), PhotoIdDataElements.ISO_23220_2_NAMESPACE,
			"age_over_99", Simple.Companion.getTRUE()));

		assertThrows(ConditionError.class, () -> ageOver.execute(env));
	}

	@Test
	public void testEvaluate_passesForAConsistentAdditionalAgeOver() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.addElement(
			photoIdCredential(), PhotoIdDataElements.ISO_23220_2_NAMESPACE,
			"age_over_21", Simple.Companion.getTRUE()));

		assertDoesNotThrow(() -> ageOver.execute(env));
	}

	/** With no birth_date disclosed there is nothing to check against. */
	@Test
	public void testEvaluate_passesWhenBirthDateAbsent() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocCredentialTestUtil.removeElement(
			photoIdCredential(), PhotoIdDataElements.ISO_23220_2_NAMESPACE, "birth_date"));

		assertDoesNotThrow(() -> ageOver.execute(env));
		assertDoesNotThrow(() -> ageInYears.execute(env));
	}
}
