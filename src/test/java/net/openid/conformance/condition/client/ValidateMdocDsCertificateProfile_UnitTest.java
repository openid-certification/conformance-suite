package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.util.TestKeysAndCerts;
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
public class ValidateMdocDsCertificateProfile_UnitTest {

	private ValidateMdocDsCertificateProfile cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocDsCertificateProfile();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	@Test
	public void testEvaluate_passesForBuiltInDsCertificate() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsForCaProfileDsCertificate() throws Exception {
		MdocCredentialTestUtil.putCredential(env, MdocDsCertificateTestFixtures.credentialWithCaProfileDsCert());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		String message = e.getMessage() + " " + e.getCause();
		assertTrue(e.getMessage().contains("profile"), message);
	}

	@Test
	public void testEvaluate_failsWhenCredentialMissing() {
		env.putString("mdoc_credential_cbor", "");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesWithMatchingTrustAnchorBinding() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));
		env.putString("credential_trust_anchor_pem", TestKeysAndCerts.IACA_ROOT_CERT_PEM);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_skipsIssuerBindingAgainstUnrelatedAnchor() throws Exception {
		// a leaf-only chain from a different IACA than the configured anchor (e.g. an issuer
		// trusted via a VICAL) must not have its issuer binding compared against that anchor -
		// doing so produced bogus issuer/AKI mismatch violations
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));
		env.putString("credential_trust_anchor_pem",
			MdocDsCertificateTestFixtures.selfSignedCertPem("CN=Unrelated Anchor"));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_requiresMdlDsEkuForMdlDocType() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocDsCertificateTestFixtures.credentialWithNonMdlEkuDsCert(DrivingLicense.MDL_DOCTYPE));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(fullError(e).contains("1.0.18013.5.1.2"), fullError(e));
	}

	@Test
	public void testEvaluate_doesNotRequireMdlDsEkuForOtherDocTypes() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocDsCertificateTestFixtures.credentialWithNonMdlEkuDsCert("eu.europa.ec.eudi.pid.1"));

		// still fails (missing AKI, issuerAltName, CRL DP, ...) but not for the mdlDS EKU OID
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(!fullError(e).contains("1.0.18013.5.1.2"), fullError(e));
	}

	private static String fullError(ConditionError e) {
		return e.getMessage() + " " + (e.getCause() != null ? e.getCause().getMessage() : "");
	}
}
