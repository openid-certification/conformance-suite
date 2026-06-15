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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureMdocMdlMandatoryDataElementsPresent_UnitTest {

	private EnsureMdocMdlMandatoryDataElementsPresent cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new EnsureMdocMdlMandatoryDataElementsPresent();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putCredential(byte[] issuerSignedBytes, String docType) {
		MdocCredentialTestUtil.putCredential(env, issuerSignedBytes);
		env.putString("mdoc_doctype", docType);
	}

	@Test
	public void testEvaluate_passesWhenAllMandatoryElementsPresent() throws Exception {
		putCredential(MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE),
			DrivingLicense.MDL_DOCTYPE);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesWhenDocTypeIsNotMdl() throws Exception {
		putCredential(MdocCredentialTestUtil.createCredentialBytes("eu.europa.ec.eudi.pid.1"),
			"eu.europa.ec.eudi.pid.1");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenMandatoryElementMissing() throws Exception {
		putCredential(MdocCredentialTestUtil.removeElement(
				MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE), "portrait"),
			DrivingLicense.MDL_DOCTYPE);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("mandatory"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenNamespaceIsEmpty() throws Exception {
		byte[] bytes = MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE);
		for (String element : EnsureMdocMdlMandatoryDataElementsPresent.MANDATORY_ELEMENTS) {
			bytes = MdocCredentialTestUtil.removeElement(bytes, element);
		}
		putCredential(bytes, DrivingLicense.MDL_DOCTYPE);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
