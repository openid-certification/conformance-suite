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
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DataItemExtensionsKt;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class MdocElementValuesAreValid_UnitTest {

	private EnsureMdocMdlElementValuesAreValid mdl;

	private EnsureMdocPhotoIdElementValuesAreValid photoId;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		mdl = new EnsureMdocMdlElementValuesAreValid();
		mdl.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		photoId = new EnsureMdocPhotoIdElementValuesAreValid();
		photoId.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putCredential(String docType, byte[] bytes) {
		MdocCredentialTestUtil.putCredential(env, bytes);
		env.putString("mdoc_doctype", docType);
	}

	/** Replaces one element's value, keeping everything else as issued. */
	private byte[] withValue(byte[] bytes, String namespace, String element, DataItem value) {
		return MdocCredentialTestUtil.replaceElementValue(bytes, namespace, element, value);
	}

	@Test
	public void testMdl_passesForCredentialTheSuiteIssues() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE,
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE));

		assertDoesNotThrow(() -> mdl.execute(env));
	}

	@Test
	public void testPhotoId_passesForCredentialTheSuiteIssues() throws Exception {
		putCredential(PhotoIdDataElements.PHOTO_ID_DOCTYPE,
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE));

		assertDoesNotThrow(() -> photoId.execute(env));
	}

	@Test
	public void testMdl_failsForCountryCodeThatIsNotAlpha2() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "issuing_country", DataItemExtensionsKt.toDataItem("Utopia")));

		ConditionError e = assertThrows(ConditionError.class, () -> mdl.execute(env));
		assertTrue(e.getMessage().contains("do not match"), e.getMessage());
	}

	@Test
	public void testMdl_failsForSexOutsideIso5218() throws Exception {
		// sex is not issued by the suite, so add it with an out of range value
		byte[] bytes = MdocCredentialTestUtil.addElement(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "sex", DataItemExtensionsKt.toDataItem(47));
		putCredential(MdlDataElements.MDL_DOCTYPE, bytes);

		assertThrows(ConditionError.class, () -> mdl.execute(env));
	}

	@Test
	public void testMdl_failsForTstrOver150Characters() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "family_name", DataItemExtensionsKt.toDataItem("x".repeat(151))));

		ConditionError e = assertThrows(ConditionError.class, () -> mdl.execute(env));
		assertTrue(e.getMessage().contains("do not match"), e.getMessage());
	}

	@Test
	public void testMdl_failsWhenBirthDateIsNotAFullDate() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "birth_date", DataItemExtensionsKt.toDataItem("1985-03-15")));

		assertThrows(ConditionError.class, () -> mdl.execute(env));
	}

	@Test
	public void testMdl_failsWhenPortraitIsNotAByteString() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "portrait", DataItemExtensionsKt.toDataItem("not a picture")));

		assertThrows(ConditionError.class, () -> mdl.execute(env));
	}


	@Test
	public void testMdl_failsWhenFullDateTextIsNotAValidDate() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "birth_date",
			new Tagged(Tagged.FULL_DATE_STRING, new Tstr("2026-99-99"))));

		ConditionError e = assertThrows(ConditionError.class, () -> mdl.execute(env));
		assertTrue(e.getMessage().contains("do not match"), e.getMessage());
	}

	@Test
	public void testMdl_failsWhenTdateTextIsNotAValidDateTime() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "expiry_date",
			new Tagged(Tagged.DATE_TIME_STRING, new Tstr("2030-01-01"))));

		assertThrows(ConditionError.class, () -> mdl.execute(env));
	}

	/** RFC 3339 permits a lowercase 't' and 'z', so those must not be rejected. */
	@Test
	public void testMdl_passesForTdateExpiryDate() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "expiry_date",
			new Tagged(Tagged.DATE_TIME_STRING, new Tstr("2030-01-01t00:00:00z"))));

		assertDoesNotThrow(() -> mdl.execute(env));
	}

	@Test
	public void testMdl_failsWhenDrivingPrivilegesIsNotAnArray() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "driving_privileges", DataItemExtensionsKt.toDataItem("B")));

		assertThrows(ConditionError.class, () -> mdl.execute(env));
	}

	@Test
	public void testMdl_failsWhenDrivingPrivilegeLacksVehicleCategoryCode() throws Exception {
		DataItem privilegeWithoutCategory = CborMap.Companion.builder().end().build();
		DataItem privileges = CborArray.Companion.builder().add(privilegeWithoutCategory).end().build();
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "driving_privileges", privileges));

		ConditionError e = assertThrows(ConditionError.class, () -> mdl.execute(env));
		assertTrue(e.getMessage().contains("do not match"), e.getMessage());
	}

	@Test
	public void testPhotoId_failsForAgeOverThatIsNotABoolean() throws Exception {
		putCredential(PhotoIdDataElements.PHOTO_ID_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE),
			PhotoIdDataElements.ISO_23220_2_NAMESPACE, "age_over_18", DataItemExtensionsKt.toDataItem(1)));

		assertThrows(ConditionError.class, () -> photoId.execute(env));
	}


	/** ISO/IEC TS 23220-2 encodes the photo ID issue_date as full_date only, unlike the mDL. */
	@Test
	public void testPhotoId_failsWhenIssueDateIsATdate() throws Exception {
		putCredential(PhotoIdDataElements.PHOTO_ID_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE),
			PhotoIdDataElements.ISO_23220_2_NAMESPACE, "issue_date",
			new Tagged(Tagged.DATE_TIME_STRING, new Tstr("2026-08-29T00:00:00Z"))));

		assertThrows(ConditionError.class, () -> photoId.execute(env));
	}

	/** ISO/IEC TS 23220-2 allows an alpha-3 code for the photo ID nationality and issuing_country. */
	@Test
	public void testPhotoId_acceptsAlpha3IssuingCountry() throws Exception {
		putCredential(PhotoIdDataElements.PHOTO_ID_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE),
			PhotoIdDataElements.ISO_23220_2_NAMESPACE, "issuing_country",
			DataItemExtensionsKt.toDataItem("USA")));

		assertDoesNotThrow(() -> photoId.execute(env));
	}

	/** 18013-5 Table 20: family_name "shall only use latin1" (ISO/IEC 8859-1) characters. */
	@Test
	public void testMdl_failsForNonLatin1FamilyName() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "family_name",
			DataItemExtensionsKt.toDataItem("\u041c\u0443\u0441\u0442\u0435\u0440\u043c\u0430\u043d\u043d")));

		assertThrows(ConditionError.class, () -> mdl.execute(env));
	}


	/** 18013-5: tdate in mDL data elements shall use "Z" and no fractions of seconds. */
	@Test
	public void testMdl_failsWhenTdateExpiryDateHasALocalOffset() throws Exception {
		putCredential(MdlDataElements.MDL_DOCTYPE, withValue(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "expiry_date",
			new Tagged(Tagged.DATE_TIME_STRING, new Tstr("2030-01-01T00:00:00+05:30"))));

		assertThrows(ConditionError.class, () -> mdl.execute(env));
	}

	/** 18013-5 Table 20 gives portrait_capture_date as tdate only, unlike issue/expiry_date. */
	@Test
	public void testMdl_failsWhenPortraitCaptureDateIsAFullDate() throws Exception {
		byte[] bytes = MdocCredentialTestUtil.addElement(
			MdocCredentialTestUtil.createCredentialBytes(MdlDataElements.MDL_DOCTYPE),
			MdlDataElements.MDL_NAMESPACE, "portrait_capture_date",
			new Tagged(Tagged.FULL_DATE_STRING, new Tstr("2026-08-29")));
		putCredential(MdlDataElements.MDL_DOCTYPE, bytes);

		assertThrows(ConditionError.class, () -> mdl.execute(env));
	}

	/** 23220-4 Table C.2: resident_state "shall only use latin1" characters. */
	@Test
	public void testPhotoId_failsForNonLatin1ResidentState() throws Exception {
		byte[] bytes = MdocCredentialTestUtil.addElement(
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE),
			PhotoIdDataElements.PHOTO_ID_NAMESPACE, "resident_state",
			DataItemExtensionsKt.toDataItem("\u5c71\u68a8\u770c"));
		putCredential(PhotoIdDataElements.PHOTO_ID_DOCTYPE, bytes);

		assertThrows(ConditionError.class, () -> photoId.execute(env));
	}

	@Test
	public void testMdl_doesNotApplyToPhotoId() throws Exception {
		putCredential(PhotoIdDataElements.PHOTO_ID_DOCTYPE,
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE));

		assertDoesNotThrow(() -> mdl.execute(env));
	}

	@Test
	public void testPhotoId_ignoresIssuerDefinedNamespace() throws Exception {
		byte[] bytes = MdocCredentialTestUtil.addElement(
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE),
			"org.iso.23220.photoid.US-IA.1", "anything", DataItemExtensionsKt.toDataItem(new byte[] { 1, 2, 3 }));
		putCredential(PhotoIdDataElements.PHOTO_ID_DOCTYPE, bytes);

		assertDoesNotThrow(() -> photoId.execute(env));
	}
}
