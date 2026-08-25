package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.util.BrainpoolSignatureProvider;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.crypto.EcCurve;
import org.multipaz.crypto.X509Cert;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocIssuerChainAgainstVical_UnitTest {

	private ValidateMdocIssuerChainAgainstVical cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private byte[] issuerSigned;

	private X509Cert issuerCert;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateMdocIssuerChainAgainstVical();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		issuerSigned = MdocCredentialTestUtil.createCredentialBytes("org.iso.18013.5.1.mDL");
		issuerCert = VicalTestFixtures.signingCertFromIssuerSigned(issuerSigned);
		MdocCredentialTestUtil.putCredential(env, issuerSigned);
	}

	private void putVical(byte[] signedVical) {
		VicalTestFixtures.putVical(env, signedVical);
	}

	@Test
	public void testEvaluate_passesWhenIssuerCertListedWithMatchingDocType() {
		putVical(VicalTestFixtures.goodSignedVical(List.of(issuerCert)));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_passesWhenCredentialChainsToBrainpoolIaca() throws Exception {
		// two IACAs in the Geneva interop VICAL use brainpool curves; the chain evaluation
		// (X509Signed.verify inside VicalTrustManager) needs BrainpoolSignatureProvider.
		// The IACA root key goes on brainpoolP256r1; the DS key stays P-256.
		BrainpoolSignatureProvider.ensureInstalled();
		VicalTestFixtures.IssuerPki pki = VicalTestFixtures.generateIssuerPki(EcCurve.BRAINPOOLP256R1);
		byte[] brainpoolChained = VicalTestFixtures.issuerSignedFromPki(pki, "org.iso.18013.5.1.mDL");
		MdocCredentialTestUtil.putCredential(env, brainpoolChained);
		putVical(VicalTestFixtures.goodSignedVical(List.of(pki.getIacaCert())));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenVicalSignatureInvalid() {
		byte[] signedVical = VicalTestFixtures.goodSignedVical(List.of(issuerCert));
		// corrupt the trailing COSE signature bytes: a VICAL that doesn't verify must not
		// silently drive a trust verdict
		signedVical[signedVical.length - 1] ^= 0x01;
		putVical(signedVical);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("signature"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenIssuerCertNotListed() {
		VicalTestFixtures.VicalSigner otherIaca = VicalTestFixtures.generateSigner();
		putVical(VicalTestFixtures.goodSignedVical(List.of(otherIaca.getCert())));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("VICAL"), e.getMessage());
	}

	@Test
	public void testEvaluate_passesWhenDocTypeListedInSecondEntryForSameIaca() {
		// nothing in Annex C forbids listing the same IACA in several entries with
		// different docTypes; the docTypes must be unioned across matching entries
		byte[] signedVical = VicalTestFixtures.sign(
			VicalTestFixtures.buildVicalMap(List.of(
				VicalTestFixtures.certificateInfo(issuerCert, List.of("eu.europa.ec.eudi.pid.1")),
				VicalTestFixtures.certificateInfo(issuerCert, List.of("org.iso.18013.5.1.mDL")))),
			VicalTestFixtures.generateSigner());
		putVical(signedVical);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenDocTypeNotListedForIssuer() {
		putVical(VicalTestFixtures.goodSignedVical(List.of(issuerCert), List.of("eu.europa.ec.eudi.pid.1")));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("document type"), e.getMessage());
	}
}
