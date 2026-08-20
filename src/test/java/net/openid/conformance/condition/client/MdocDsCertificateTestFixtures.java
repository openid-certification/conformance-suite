package net.openid.conformance.condition.client;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.multipaz.documenttype.knowntypes.DrivingLicense;
import org.multipaz.testapp.VciMdocUtils;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * Builds mdoc IssuerSigned test credentials whose x5chain carries a deliberately
 * non-conformant document signer certificate, for exercising the ISO 18013-5 Table B.3
 * profile checks.
 */
final class MdocDsCertificateTestFixtures {

	private MdocDsCertificateTestFixtures() {
		// utility class
	}

	/**
	 * Creates an IssuerSigned credential signed with a self-signed certificate carrying the CA
	 * profile the suite used to (mis)issue before #1891: critical basicConstraints CA:true and
	 * critical keyUsage keyCertSign+cRLSign, no EKU/AKI/issuerAltName/CRL distribution points.
	 */
	static byte[] credentialWithCaProfileDsCert() throws Exception {
		return credentialSignedWith(DrivingLicense.MDL_DOCTYPE, builder -> {
			builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
			builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
		});
	}

	/**
	 * Creates an IssuerSigned credential for the given docType, signed with a certificate that
	 * has a digitalSignature-only keyUsage but a critical extended key usage of id-kp-clientAuth
	 * instead of mdlDS (and none of the other Table B.3 extensions).
	 */
	static byte[] credentialWithNonMdlEkuDsCert(String docType) throws Exception {
		return credentialSignedWith(docType, builder -> {
			builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
			builder.addExtension(Extension.extendedKeyUsage, true,
				new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));
		});
	}

	private interface ExtensionCustomizer {
		void customize(X509v3CertificateBuilder builder) throws Exception;
	}

	private static byte[] credentialSignedWith(String docType, ExtensionCustomizer customizer) throws Exception {
		ECKey signingKey = new ECKeyGenerator(Curve.P_256).generate();

		X500Name name = new X500Name("CN=Bad DS Cert");
		X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
			name,
			BigInteger.valueOf(System.nanoTime()),
			new Date(System.currentTimeMillis() - 60_000),
			new Date(System.currentTimeMillis() + 3600_000),
			name,
			SubjectPublicKeyInfo.getInstance(signingKey.toECPublicKey().getEncoded()));
		customizer.customize(builder);
		byte[] certDer = builder
			.build(new JcaContentSignerBuilder("SHA256withECDSA").build(signingKey.toECPrivateKey()))
			.getEncoded();

		ECKey signingKeyWithX5c = new ECKey.Builder(signingKey)
			.x509CertChain(List.of(Base64.encode(certDer)))
			.build();
		String deviceKeyJson = new ECKeyGenerator(Curve.P_256).generate().toJSONString();

		String mdocBase64Url = VciMdocUtils.createMdocCredential(
			deviceKeyJson, docType, signingKeyWithX5c.toJSONString());
		return new Base64URL(mdocBase64Url).decode();
	}
}
