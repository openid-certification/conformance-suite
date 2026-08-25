package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.PhotoIdDataElements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.documenttype.knowntypes.DrivingLicense;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DataItemExtensionsKt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureMdocPhotoIdConditionalDataElementsPresent_UnitTest {

	private EnsureMdocPhotoIdConditionalDataElementsPresent cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new EnsureMdocPhotoIdConditionalDataElementsPresent();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putCredential(byte[] issuerSignedBytes, String docType) {
		MdocCredentialTestUtil.putCredential(env, issuerSignedBytes);
		env.putString("mdoc_doctype", docType);
	}

	private static DataItem bstr() {
		return DataItemExtensionsKt.toDataItem(new byte[] { 1, 2, 3 });
	}

	/** Adds the given elements to the org.iso.23220.datagroups.1 namespace. */
	private static byte[] withDataGroups(byte[] bytes, String... elements) {
		for (String element : elements) {
			bytes = MdocCredentialTestUtil.addElement(bytes,
				PhotoIdDataElements.DATAGROUPS_NAMESPACE, element, bstr());
		}
		return bytes;
	}

	@Test
	public void testEvaluate_passesWhenDatagroupsNamespaceNotUsed() throws Exception {
		putCredential(MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE),
			PhotoIdDataElements.PHOTO_ID_DOCTYPE);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesWhenDocTypeIsNotPhotoId() throws Exception {
		putCredential(MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE),
			DrivingLicense.MDL_DOCTYPE);

		assertDoesNotThrow(() -> cond.execute(env));
	}


	/** Table C.3 requires dg1, dg2 and sod "when this namespace exists" — even when it is empty. */
	@Test
	public void testEvaluate_failsWhenDatagroupsNamespaceIsEmpty() throws Exception {
		byte[] bytes = MdocCredentialTestUtil.addEmptyNamespace(
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE),
			PhotoIdDataElements.DATAGROUPS_NAMESPACE);
		putCredential(bytes, PhotoIdDataElements.PHOTO_ID_DOCTYPE);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("dg1, dg2 and sod"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenDatagroupsNamespaceIsIncomplete() throws Exception {
		byte[] bytes = withDataGroups(
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE), "dg1");
		putCredential(bytes, PhotoIdDataElements.PHOTO_ID_DOCTYPE);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("dg1, dg2 and sod"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenTravelDocumentElementsMissing() throws Exception {
		byte[] bytes = withDataGroups(
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE),
			"dg1", "dg2", "sod");
		putCredential(bytes, PhotoIdDataElements.PHOTO_ID_DOCTYPE);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("travel_document_type and travel_document_mrz"), e.getMessage());
	}

	@Test
	public void testEvaluate_passesWhenDatagroupsAndTravelDocumentElementsPresent() throws Exception {
		byte[] bytes = withDataGroups(
			MdocCredentialTestUtil.createCredentialBytes(PhotoIdDataElements.PHOTO_ID_DOCTYPE),
			"dg1", "dg2", "sod");
		bytes = MdocCredentialTestUtil.addElement(bytes, PhotoIdDataElements.PHOTO_ID_NAMESPACE,
			"travel_document_type", DataItemExtensionsKt.toDataItem("P"));
		bytes = MdocCredentialTestUtil.addElement(bytes, PhotoIdDataElements.PHOTO_ID_NAMESPACE,
			"travel_document_mrz", DataItemExtensionsKt.toDataItem("P<UTOMUSTERMANN<<ERIKA<<<<<<<<<<<<<<<<<<<<<<"));
		putCredential(bytes, PhotoIdDataElements.PHOTO_ID_DOCTYPE);

		assertDoesNotThrow(() -> cond.execute(env));
	}
}
