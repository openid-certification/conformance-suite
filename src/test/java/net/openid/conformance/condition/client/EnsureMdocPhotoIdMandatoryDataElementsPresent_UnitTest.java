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
import org.multipaz.documenttype.knowntypes.DrivingLicense;
import org.multipaz.documenttype.knowntypes.PhotoID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureMdocPhotoIdMandatoryDataElementsPresent_UnitTest {

	private EnsureMdocPhotoIdMandatoryDataElementsPresent cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new EnsureMdocPhotoIdMandatoryDataElementsPresent();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putCredential(byte[] issuerSignedBytes, String docType) {
		MdocCredentialTestUtil.putCredential(env, issuerSignedBytes);
		env.putString("mdoc_doctype", docType);
	}

	@Test
	public void testEvaluate_passesWhenAllMandatoryElementsPresent() throws Exception {
		putCredential(MdocCredentialTestUtil.createCredentialBytes(PhotoID.PHOTO_ID_DOCTYPE),
			PhotoID.PHOTO_ID_DOCTYPE);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesWhenDocTypeIsNotPhotoId() throws Exception {
		putCredential(MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE),
			DrivingLicense.MDL_DOCTYPE);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenMandatoryElementMissing() throws Exception {
		putCredential(MdocCredentialTestUtil.removeElement(
				MdocCredentialTestUtil.createCredentialBytes(PhotoID.PHOTO_ID_DOCTYPE),
				PhotoID.ISO_23220_2_NAMESPACE, "portrait"),
			PhotoID.PHOTO_ID_DOCTYPE);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("mandatory"), e.getMessage());
	}

	@Test
	public void testEvaluate_recommendedElementsPresentInIssuedCredential() throws Exception {
		EnsureMdocPhotoIdRecommendedDataElementsPresent recommended =
			new EnsureMdocPhotoIdRecommendedDataElementsPresent();
		recommended.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		putCredential(MdocCredentialTestUtil.createCredentialBytes(PhotoID.PHOTO_ID_DOCTYPE),
			PhotoID.PHOTO_ID_DOCTYPE);

		assertDoesNotThrow(() -> recommended.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenRecommendedElementMissing() throws Exception {
		EnsureMdocPhotoIdRecommendedDataElementsPresent recommended =
			new EnsureMdocPhotoIdRecommendedDataElementsPresent();
		recommended.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		putCredential(MdocCredentialTestUtil.removeElement(
				MdocCredentialTestUtil.createCredentialBytes(PhotoID.PHOTO_ID_DOCTYPE),
				PhotoID.ISO_23220_2_NAMESPACE, "age_in_years"),
			PhotoID.PHOTO_ID_DOCTYPE);

		ConditionError e = assertThrows(ConditionError.class, () -> recommended.execute(env));
		assertTrue(e.getMessage().contains("recommended"), e.getMessage());
	}

	/**
	 * ISO/IEC TS 23220-2 ed.2 replaced the previous edition's "_unicode" suffixed element
	 * identifiers with the plain ones, so a credential using the old names is missing the
	 * mandatory elements as far as this check is concerned.
	 */
	@Test
	public void testEvaluate_failsWhenPreviousEditionUnicodeNamesUsed() throws Exception {
		byte[] bytes = MdocCredentialTestUtil.createCredentialBytes(PhotoID.PHOTO_ID_DOCTYPE);
		bytes = MdocCredentialTestUtil.removeElement(bytes, PhotoID.ISO_23220_2_NAMESPACE, "family_name");
		putCredential(bytes, PhotoID.PHOTO_ID_DOCTYPE);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("mandatory"), e.getMessage());
	}
}
