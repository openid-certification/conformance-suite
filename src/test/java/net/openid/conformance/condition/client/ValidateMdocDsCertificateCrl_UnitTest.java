package net.openid.conformance.condition.client;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocIacaCrl;
import net.openid.conformance.util.TestKeysAndCerts;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.multipaz.documenttype.knowntypes.DrivingLicense;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fetched CRL against ISO/IEC 18013-5 Table B.10 and B.2, for the suite's own DS
 * certificate with the IACA configured as the trust anchor.
 */
@ExtendWith(MockitoExtension.class)
public class ValidateMdocDsCertificateCrl_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateMdocDsCertificateCrl cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateMdocDsCertificateCrl();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
		MdocCredentialTestUtil.putCredential(env,
			MdocCredentialTestUtil.createCredentialBytes(DrivingLicense.MDL_DOCTYPE));
		env.putString("credential_trust_anchor_pem", TestKeysAndCerts.IACA_ROOT_CERT_PEM);
	}

	@Test
	public void passesForTheCrlTheSuiteServes() {
		MdocCrlTestFixtures.putCrl(env, MdocIacaCrl.generate());
		assertDoesNotThrow(() -> cond.execute(env));
		assertEquals("true", env.getString(AbstractMdocDsCertificateCrlCondition.ENV_AUTHENTICATED));
	}

	@Test
	public void passesForAConformantCrlListingAnotherCertificate() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().revoke(BigInteger.valueOf(12345)).build());
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void passesWithoutAnIssuerToVerifyTheSignatureAgainst() throws Exception {
		// no trust anchor and no VICAL: the signature cannot be checked, the rest still is
		env.removeNativeValue("credential_trust_anchor_pem");
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().build());
		assertDoesNotThrow(() -> cond.execute(env));
		// unauthenticated, so revocation status is not read from it
		assertNull(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_AUTHENTICATED));
	}

	@Test
	public void failsWhenTheSignatureIsNotTheIssuingIacas() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl()
			.signer(new ECKeyGenerator(Curve.P_256).generate().toECPrivateKey()).build());
		assertViolation("signature");
	}

	@Test
	public void failsWhenTheIssuerIsNotTheDsCertificatesIssuer() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().issuer("CN=Some Other IACA,C=US").build());
		assertViolation("issuer");
	}

	@Test
	public void failsWhenNextUpdateHasPassed() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl()
			.thisUpdate(Instant.now().minus(Duration.ofDays(100)))
			.nextUpdate(Instant.now().minus(Duration.ofDays(10))).build());
		assertViolation("nextUpdate");
	}

	@Test
	public void leavesANextUpdateMoreThan90DaysOutToItsOwnCheck() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().nextUpdate(Instant.now().plus(Duration.ofDays(91))).build());
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenNextUpdateIsAbsent() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().nextUpdate(null).build());
		assertViolation("nextUpdate");
	}

	@Test
	public void toleratesAnIssuerClockSlightlyAhead() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().thisUpdate(Instant.now().plus(Duration.ofSeconds(30))).build());
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void toleratesANextUpdateThatPassedMomentsAgo() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().nextUpdate(Instant.now().minus(Duration.ofSeconds(30))).build());
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenThisUpdateIsInTheFuture() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl()
			.thisUpdate(Instant.now().plus(Duration.ofDays(1)))
			.nextUpdate(Instant.now().plus(Duration.ofDays(8))).build());
		assertViolation("thisUpdate");
	}

	@Test
	public void failsWhenTheSignatureAlgorithmIsNotEcdsaWithSha2() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().signatureAlgorithm("SHA1withECDSA").build());
		assertViolation("algorithm");
	}

	@Test
	public void failsWhenTheAuthorityKeyIdentifierIsMissing() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().authorityKeyIdentifier(null).build());
		assertViolation("authority key identifier");
	}

	@Test
	public void failsWhenTheAuthorityKeyIdentifierHasNoKeyIdentifier() throws Exception {
		// the issuer-and-serial form of the extension, without a keyIdentifier
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().authorityKeyIdentifier(null)
			.extension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(
				new GeneralNames(new GeneralName(new X500Name("CN=Some IACA,C=US"))), BigInteger.ONE))
			.build());
		assertViolation("no keyIdentifier");
	}

	@Test
	public void failsWhenTheAuthorityKeyIdentifierDoesNotMatchTheDsCertificates() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().authorityKeyIdentifier(new byte[] { 1, 2, 3, 4 }).build());
		assertViolation("authority key identifier");
	}

	@Test
	public void failsWhenTheCrlNumberIsMissing() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().withoutCrlNumber().build());
		assertViolation("CRL number");
	}

	@Test
	public void failsWhenTheCrlNumberIsNegative() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().crlNumber(BigInteger.valueOf(-1)).build());
		assertViolation("is negative");
	}

	@Test
	public void failsWhenTheCrlNumberIsLongerThan20Octets() throws Exception {
		// 2^167 encodes as 22 content octets: 21 for the magnitude plus a leading zero sign octet
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().crlNumber(BigInteger.ONE.shiftLeft(167)).build());
		assertViolation("22 octets");
	}

	@Test
	public void passesForATwentyOctetCrlNumber() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().crlNumber(BigInteger.ONE.shiftLeft(158)).build());
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsOnAnyFurtherExtension() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl()
			.extension(Extension.issuingDistributionPoint, true, new IssuingDistributionPoint(null, false, false))
			.build());
		assertViolation("extension");
	}

	@Test
	public void failsOnAnEmptyRevokedCertificatesList() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().withEmptyRevokedList().build());
		assertViolation("present but empty");
	}

	@Test
	public void failsOnCrlEntryExtensions() throws Exception {
		MdocCrlTestFixtures.putCrl(env, MdocCrlTestFixtures.iacaCrl().revoke(BigInteger.valueOf(7)).withEntryExtensions().build());
		assertViolation("entry extension");
	}

	@Test
	public void failsWhenTheCrlDoesNotParse() {
		MdocCrlTestFixtures.putCrl(env, new byte[] { 0x30, 0x03, 0x02, 0x01, 0x01 });
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsOnAPemEncodedCrl() {
		String pem = "-----BEGIN X509 CRL-----\n"
			+ Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(MdocIacaCrl.generate())
			+ "\n-----END X509 CRL-----\n";
		MdocCrlTestFixtures.putCrl(env, pem.getBytes(StandardCharsets.US_ASCII));
		assertViolation("PEM");
	}

	@Test
	public void failsOnTwoConcatenatedCrls() throws Exception {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		body.write(MdocIacaCrl.generate());
		body.write(MdocCrlTestFixtures.iacaCrl().build());
		MdocCrlTestFixtures.putCrl(env, body.toByteArray());
		assertViolation("single DER encoded CRL");
	}

	@Test
	public void failsOnDataAfterTheCrl() throws Exception {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		body.write(MdocIacaCrl.generate());
		body.write('\n');
		MdocCrlTestFixtures.putCrl(env, body.toByteArray());
		assertViolation("single DER encoded CRL");
	}

	@Test
	public void failsOnACrlThatIsBerButNotDerEncoded() throws Exception {
		// the same CRL with the outer SEQUENCE in indefinite-length form, which DER forbids
		ASN1Sequence der = ASN1Sequence.getInstance(MdocIacaCrl.generate());
		byte[] ber = new BERSequence(der.toArray()).getEncoded();
		assertEquals((byte) 0x80, ber[1], "the fixture is not indefinite-length encoded");
		MdocCrlTestFixtures.putCrl(env, ber);
		assertViolation("not DER encoded");
	}

	private void assertViolation(String expectedWording) {
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains(expectedWording), e.getMessage());
		assertNull(env.getString(AbstractMdocDsCertificateCrlCondition.ENV_AUTHENTICATED));
	}
}
