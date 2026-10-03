package org.multipaz.testapp;

import com.nimbusds.jose.jwk.Curve;
import net.openid.conformance.util.MdocUtil;
import net.openid.conformance.util.TestKeysAndCerts;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.documenttype.knowntypes.DrivingLicense;
import org.multipaz.mdoc.mso.MobileSecurityObject;
import org.multipaz.revocation.RevocationStatus;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VciMdocUtils_UnitTest {

	private static String deviceKeyJson() throws Exception {
		return new ECKeyGenerator(Curve.P_256).generate().toJSONString();
	}

	/** Self-signs a throwaway certificate for the given key so the JWK can carry an x5c. */
	private static byte[] selfSignedCertDer(ECKey ecKey) throws Exception {
		X500Name name = new X500Name("CN=VciMdocUtils Unit Test");
		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
			name,
			BigInteger.valueOf(System.nanoTime()),
			new Date(System.currentTimeMillis() - 60_000),
			new Date(System.currentTimeMillis() + 3600_000),
			name,
			SubjectPublicKeyInfo.getInstance(ecKey.toECPublicKey().getEncoded()));
		return builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(ecKey.toECPrivateKey())).getEncoded();
	}

	private static X509CertChain extractX5chain(String mdocBase64Url) {
		byte[] issuerSignedBytes = new Base64URL(mdocBase64Url).decode();
		DataItem issuerSigned = Cbor.INSTANCE.decode(issuerSignedBytes);
		CoseSign1 coseSign1 = issuerSigned.getOrNull("issuerAuth").getAsCoseSign1();
		DataItem x5chainItem = coseSign1.getUnprotectedHeaders().get(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN));
		return x5chainItem.getAsX509CertChain();
	}

	@Test
	public void configuredJwkWithMultiCertX5c_embedsWholeChain() throws Exception {
		ECKey issuerKey = new ECKeyGenerator(Curve.P_256).generate();
		byte[] leafDer = selfSignedCertDer(issuerKey);
		byte[] intermediateDer = bytes(TestKeysAndCerts.getIacaRootCert().getEncoded());
		ECKey issuerKeyWithChain = new ECKey.Builder(issuerKey)
			.x509CertChain(List.of(Base64.encode(leafDer), Base64.encode(intermediateDer)))
			.build();

		String mdoc = VciMdocUtils.createMdocCredential(
			deviceKeyJson(), DrivingLicense.MDL_DOCTYPE, issuerKeyWithChain.toJSONString());

		X509CertChain x5chain = extractX5chain(mdoc);
		assertThat(x5chain.getCertificates()).hasSize(2);
		assertThat(bytes(x5chain.getCertificates().get(0).getEncoded())).isEqualTo(leafDer);
		assertThat(bytes(x5chain.getCertificates().get(1).getEncoded())).isEqualTo(intermediateDer);
	}

	@Test
	public void configuredJwkWithoutX5c_isRejected() throws Exception {
		String issuerJwkWithoutX5c = new ECKeyGenerator(Curve.P_256).generate().toJSONString();

		assertThatThrownBy(() -> VciMdocUtils.createMdocCredential(
			deviceKeyJson(), DrivingLicense.MDL_DOCTYPE, issuerJwkWithoutX5c))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("x5c");
	}

	@Test
	public void noConfiguredJwk_usesBuiltInDocumentSignerCertificate() throws Exception {
		String mdoc = VciMdocUtils.createMdocCredential(deviceKeyJson(), DrivingLicense.MDL_DOCTYPE, null);

		X509CertChain x5chain = extractX5chain(mdoc);
		assertThat(x5chain.getCertificates()).hasSize(1);
		assertThat(bytes(x5chain.getCertificates().get(0).getEncoded()))
			.isEqualTo(bytes(TestKeysAndCerts.getDocumentSignerCert().getEncoded()));
	}

	@Test
	public void noRevocationReference_msoIsShortLivedButDocumentExpiresInAYear() throws Exception {
		// 2026-01-15T10:00:00Z
		long signedAt = 1768471200L;

		String mdoc = VciMdocUtils.createMdocCredential(deviceKeyJson(), DrivingLicense.MDL_DOCTYPE, null, signedAt);

		// with no way to revoke it the MSO is short-lived: CIR (EU) 2024/2979 only exempts
		// attestations valid for 24 hours or less from the revocation requirement
		MobileSecurityObject mso = MdocUtil.parseMso(decode(mdoc));
		assertThat(mso.getRevocationStatus()).isNull();
		assertThat(mso.getValidUntil().getEpochSeconds() - signedAt).isEqualTo(Duration.ofHours(23).toSeconds());
		// the document's administrative validity is unrelated to the MSO's and is still a year
		// (ISO/IEC 18013-5 9.1.2.4 NOTE 2)
		assertThat(expiryDate(mdoc, "org.iso.18013.5.1")).isEqualTo("2027-01-15");
	}

	@Test
	public void statusListReference_msoIsValidUntilTheDocumentExpires() throws Exception {
		long signedAt = 1768471200L;

		String mdoc = VciMdocUtils.createMdocCredential(deviceKeyJson(), DrivingLicense.MDL_DOCTYPE, null, signedAt,
			"https://example.com/statuslists/1", 7L);

		// the status list is what allows the MSO to outlive the 24 hour exemption
		MobileSecurityObject mso = MdocUtil.parseMso(decode(mdoc));
		assertThat(mso.getRevocationStatus()).isInstanceOf(RevocationStatus.StatusList.class);
		assertThat(mso.getValidUntil().getEpochSeconds() - signedAt).isEqualTo(Duration.ofDays(365).toSeconds());
		assertThat(expiryDate(mdoc, "org.iso.18013.5.1")).isEqualTo("2027-01-15");
	}

	private static DataItem decode(String mdocBase64Url) {
		return Cbor.INSTANCE.decode(new Base64URL(mdocBase64Url).decode());
	}

	private static String expiryDate(String mdocBase64Url, String namespace) throws Exception {
		for (DataItem issuerSignedItemBytes : MdocUtil.getIssuerSignedItems(decode(mdocBase64Url)).get(namespace)) {
			DataItem issuerSignedItem = issuerSignedItemBytes.getAsTaggedEncodedCbor();
			if ("expiry_date".equals(issuerSignedItem.get("elementIdentifier").getAsTstr())) {
				return issuerSignedItem.get("elementValue").getAsTagged().getAsTstr();
			}
		}
		throw new AssertionError("no expiry_date element in namespace " + namespace);
	}

	private static byte[] bytes(kotlinx.io.bytestring.ByteString byteString) {
		return byteString.toByteArray(0, byteString.getSize());
	}
}
