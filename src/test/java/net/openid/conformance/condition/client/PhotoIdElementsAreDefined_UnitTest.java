package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import org.multipaz.documenttype.knowntypes.DrivingLicense;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class PhotoIdElementsAreDefined_UnitTest {

	private EnsurePresentedMdocPhotoIdElementsAreDefined presented;

	private EnsureIssuedMdocPhotoIdElementsAreDefined issued;

	private EnsurePresentedMdocMdlElementsAreDefined presentedMdl;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		presented = new EnsurePresentedMdocPhotoIdElementsAreDefined();
		presented.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		issued = new EnsureIssuedMdocPhotoIdElementsAreDefined();
		issued.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		presentedMdl = new EnsurePresentedMdocMdlElementsAreDefined();
		presentedMdl.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	private void putPresentedMdoc(String docType, String namespace, String... elements) {
		JsonArray array = new JsonArray();
		for (String element : elements) {
			array.add(element);
		}
		JsonObject disclosed = new JsonObject();
		disclosed.add(namespace, array);
		JsonObject mdoc = new JsonObject();
		mdoc.addProperty("docType", docType);
		mdoc.add("disclosed_elements", disclosed);
		env.putObject("mdoc", mdoc);
	}

	@Test
	public void testPresented_passesForDefinedElements() {
		putPresentedMdoc(PhotoIdDataElements.PHOTO_ID_DOCTYPE, PhotoIdDataElements.ISO_23220_2_NAMESPACE,
			"family_name", "portrait", "age_over_21");

		assertDoesNotThrow(() -> presented.execute(env));
	}

	@Test
	public void testPresented_failsForUndefinedElement() {
		putPresentedMdoc(PhotoIdDataElements.PHOTO_ID_DOCTYPE, PhotoIdDataElements.ISO_23220_2_NAMESPACE,
			"family_name", "favourite_colour");

		ConditionError e = assertThrows(ConditionError.class, () -> presented.execute(env));
		assertTrue(e.getMessage().contains("does not define"), e.getMessage());
	}

	/**
	 * ISO/IEC TS 23220-2 ed.2 replaced the previous edition's "_unicode" identifiers, so a wallet
	 * still using them is presenting elements Annex C no longer defines.
	 */
	@Test
	public void testPresented_failsForPreviousEditionUnicodeName() {
		putPresentedMdoc(PhotoIdDataElements.PHOTO_ID_DOCTYPE, PhotoIdDataElements.ISO_23220_2_NAMESPACE,
			"family_name_unicode");

		assertThrows(ConditionError.class, () -> presented.execute(env));
	}

	/** Annex C requirement NS_dual explicitly permits issuer defined namespaces. */
	@Test
	public void testPresented_allowsAdditionalNamespace() {
		putPresentedMdoc(PhotoIdDataElements.PHOTO_ID_DOCTYPE, "org.iso.23220.photoid.US-IA.1",
			"anything_the_issuer_likes");

		assertDoesNotThrow(() -> presented.execute(env));
	}

	@Test
	public void testPresented_passesForOtherDocType() {
		putPresentedMdoc(DrivingLicense.MDL_DOCTYPE, DrivingLicense.MDL_NAMESPACE, "un_distinguishing_sign");

		assertDoesNotThrow(() -> presented.execute(env));
	}

	/**
	 * ISO/IEC DTS 23220-4:2025 Table C.3 puts the ICAO data groups in org.iso.23220.datagroups.1
	 * with unprefixed identifiers. The 2024 draft used org.iso.23220.dtc.1 with "dtc_" prefixed
	 * identifiers, which the current draft no longer defines.
	 */
	@Test
	public void testPresented_datagroupsNamespaceUsesUnprefixedNames() {
		putPresentedMdoc(PhotoIdDataElements.PHOTO_ID_DOCTYPE, PhotoIdDataElements.DATAGROUPS_NAMESPACE,
			"dg1", "dg2", "sod", "version");
		assertDoesNotThrow(() -> presented.execute(env));

		putPresentedMdoc(PhotoIdDataElements.PHOTO_ID_DOCTYPE, PhotoIdDataElements.DATAGROUPS_NAMESPACE,
			"dtc_dg1", "dtc_sod");
		assertThrows(ConditionError.class, () -> presented.execute(env));
	}

	@Test
	public void testPresented_allowsTravelDocumentElements() {
		putPresentedMdoc(PhotoIdDataElements.PHOTO_ID_DOCTYPE, PhotoIdDataElements.PHOTO_ID_NAMESPACE,
			"travel_document_type", "travel_document_number", "travel_document_mrz");

		assertDoesNotThrow(() -> presented.execute(env));
	}

	@Test
	public void testPresented_allowsEnrolmentPortraitImage() {
		putPresentedMdoc(PhotoIdDataElements.PHOTO_ID_DOCTYPE, PhotoIdDataElements.ISO_23220_2_NAMESPACE,
			"enrolment_portrait_image", "family_name_viz", "given_name_viz");

		assertDoesNotThrow(() -> presented.execute(env));
	}

	@Test
	public void testPresentedMdl_passesForDefinedElements() {
		putPresentedMdoc(MdlDataElements.MDL_DOCTYPE, MdlDataElements.MDL_NAMESPACE,
			"family_name", "driving_privileges", "un_distinguishing_sign", "age_over_21",
			"biometric_template_signature_sign");

		assertDoesNotThrow(() -> presentedMdl.execute(env));
	}

	@Test
	public void testPresentedMdl_failsForUndefinedElement() {
		putPresentedMdoc(MdlDataElements.MDL_DOCTYPE, MdlDataElements.MDL_NAMESPACE,
			"family_name", "favourite_colour");

		ConditionError e = assertThrows(ConditionError.class, () -> presentedMdl.execute(env));
		assertTrue(e.getMessage().contains("does not define"), e.getMessage());
	}

	/** ISO/IEC 18013-5 13.4.9 lets an issuing authority put domestic data in its own namespace. */
	@Test
	public void testPresentedMdl_allowsIssuingAuthorityNamespace() {
		putPresentedMdoc(MdlDataElements.MDL_DOCTYPE, "org.iso.18013.5.1.US", "organ_donor");

		assertDoesNotThrow(() -> presentedMdl.execute(env));
	}

	@Test
	public void testPresentedMdl_doesNotApplyToPhotoId() {
		putPresentedMdoc(PhotoIdDataElements.PHOTO_ID_DOCTYPE, PhotoIdDataElements.ISO_23220_2_NAMESPACE,
			"family_name");

		assertDoesNotThrow(() -> presentedMdl.execute(env));
	}

	@Test
	public void testIssued_passesForCredentialTheSuiteIssues() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE));
		env.putString("mdoc_doctype", PhotoIdDataElements.PHOTO_ID_DOCTYPE);

		assertDoesNotThrow(() -> issued.execute(env));
	}
}
