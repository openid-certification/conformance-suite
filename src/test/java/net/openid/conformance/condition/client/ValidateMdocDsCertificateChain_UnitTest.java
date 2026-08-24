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
public class ValidateMdocDsCertificateChain_UnitTest {

	private ValidateMdocDsCertificateChain cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocDsCertificateChain();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void testEvaluate_passesForBuiltInDsCertAgainstIacaRootTrustAnchor() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));
		env.putString("credential_trust_anchor_pem", TestKeysAndCerts.IACA_ROOT_CERT_PEM);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesReducedChecksWithoutTrustAnchor() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_rejectsSelfSignedDsCertEvenWithoutTrustAnchor() throws Exception {
		// the pre-#1891 suite certificate scenario: a self-signed DS cert in x5chain
		MdocCredentialTestUtil.putCredential(env, MdocDsCertificateTestFixtures.credentialWithCaProfileDsCert());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("self-signed"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsPkixValidationAgainstUnrelatedTrustAnchor() throws Exception {
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));
		// unrelated self-signed cert as the configured trust anchor
		env.putString("credential_trust_anchor_pem",
			MdocDsCertificateTestFixtures.selfSignedCertPem("CN=Unrelated Trust Anchor"));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("PKIX"), e.getMessage());
	}
}
