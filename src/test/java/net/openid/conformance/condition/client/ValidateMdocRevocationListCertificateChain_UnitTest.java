package net.openid.conformance.condition.client;

import kotlin.Pair;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.crypto.EcPrivateKey;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateMdocRevocationListCertificateChain_UnitTest {

	private static final String URI = "https://example.com/statuslists/1";

	private ValidateMdocRevocationListCertificateChain cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VicalTestFixtures.IssuerPki pki;

	@BeforeEach
	public void setUp() {
		cond = new ValidateMdocRevocationListCertificateChain();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
		pki = VicalTestFixtures.generateIssuerPki();
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(
			VicalTestFixtures.issuerSignedFromPki(pki, "org.iso.18013.5.1.mDL")));
	}

	private void putStatusListTokenSignedUnder(VicalTestFixtures.IssuerPki issuerPki) throws Exception {
		Pair<EcPrivateKey, X509Cert> signer =
			VicalTestFixtures.mintLeafUnderIaca(issuerPki, "Status List Signer");
		byte[] token = StatusListCwtTestFixtures.statusListTokenSignedBy(URI, signer.getFirst(),
			new X509CertChain(List.of(signer.getSecond())));
		env.putString(AbstractRevocationListCwtCondition.ENV_TOKEN,
			StatusListCwtTestFixtures.encode(token));
	}

	@Test
	public void testEvaluate_passesWhenSignedUnderTheSameIaca() throws Exception {
		putStatusListTokenSignedUnder(pki);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenSignedUnderADifferentCa() throws Exception {
		putStatusListTokenSignedUnder(VicalTestFixtures.generateIssuerPki());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("same CA"), e.getMessage());
	}

	@Test
	public void testEvaluate_passesWithMatchingTrustAnchor() throws Exception {
		putStatusListTokenSignedUnder(pki);
		env.putString("credential_trust_anchor_pem", VicalTestFixtures.toPem(pki.getIacaCert()));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWithWrongTrustAnchor() throws Exception {
		putStatusListTokenSignedUnder(pki);
		// same DN as the mdoc's IACA (fixtures reuse the name) but a different key, so the
		// same-issuer check passes and the PKIX validation against the anchor must catch it
		VicalTestFixtures.IssuerPki otherPki = VicalTestFixtures.generateIssuerPki();
		env.putString("credential_trust_anchor_pem", VicalTestFixtures.toPem(otherPki.getIacaCert()));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("Credential Trust Anchor"), e.getMessage());
	}

	@Test
	public void testEvaluate_passesAgainstTheIacaTheVicalListsForTheMdoc() throws Exception {
		putStatusListTokenSignedUnder(pki);
		configureVicalResolvingTo(pki.getIacaCert());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenTheVicalIacaDidNotSignTheListSigner() throws Exception {
		// the forged leaf names the mdoc's IACA as its issuer and carries its key identifier, so
		// the structural same-issuer check passes; only a signature check against the IACA the
		// VICAL lists can catch that another key signed it
		VicalTestFixtures.IssuerPki otherPki = VicalTestFixtures.generateIssuerPki();
		Pair<EcPrivateKey, X509Cert> forged =
			VicalTestFixtures.mintLeafClaimingIaca(pki, otherPki.getIacaKey(), "Forged Status List Signer");
		env.putString(AbstractRevocationListCwtCondition.ENV_TOKEN, StatusListCwtTestFixtures.encode(
			StatusListCwtTestFixtures.statusListTokenSignedBy(URI, forged.getFirst(),
				new X509CertChain(List.of(forged.getSecond())))));
		configureVicalResolvingTo(pki.getIacaCert());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("VICAL"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenTheMdocDidNotResolveToAVicalIaca() throws Exception {
		putStatusListTokenSignedUnder(pki);
		VicalTestFixtures.putVical(env, VicalTestFixtures.goodSignedVical(List.of(pki.getIacaCert())));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("did not resolve"), e.getMessage());
	}

	/** A configured VICAL whose evaluation of the mdoc's chain resolved to the given IACA. */
	private void configureVicalResolvingTo(X509Cert iaca) {
		VicalTestFixtures.putVical(env, VicalTestFixtures.goodSignedVical(List.of(iaca)));
		env.putString(ValidateMdocIssuerChainAgainstVical.ENV_TRUST_POINT, Base64.getEncoder()
			.encodeToString(iaca.getEncoded().toByteArray(0, iaca.getEncoded().getSize())));
	}

	@Test
	public void testEvaluate_certificateElementIsTheTrustPointEvenForADifferentCa() throws Exception {
		// with the Certificate element present, the chain may be under a different CA than the
		// MSO's (12.3.6.2); the element itself is the trust point
		VicalTestFixtures.IssuerPki otherPki = VicalTestFixtures.generateIssuerPki();
		putStatusListTokenSignedUnder(otherPki);
		byte[] otherIacaDer = otherPki.getIacaCert().getEncoded()
			.toByteArray(0, otherPki.getIacaCert().getEncoded().getSize());
		env.putString(AbstractRevocationListCwtCondition.ENV_REFERENCE_CERTIFICATE,
			Base64.getEncoder().encodeToString(otherIacaDer));

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenCertificateElementDoesNotCoverTheChain() throws Exception {
		putStatusListTokenSignedUnder(pki);
		VicalTestFixtures.IssuerPki otherPki = VicalTestFixtures.generateIssuerPki();
		byte[] otherIacaDer = otherPki.getIacaCert().getEncoded()
			.toByteArray(0, otherPki.getIacaCert().getEncoded().getSize());
		env.putString(AbstractRevocationListCwtCondition.ENV_REFERENCE_CERTIFICATE,
			Base64.getEncoder().encodeToString(otherIacaDer));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("Certificate element"), e.getMessage());
	}
}
