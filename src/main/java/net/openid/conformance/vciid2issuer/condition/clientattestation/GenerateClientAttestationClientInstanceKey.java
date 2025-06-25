package net.openid.conformance.vciid2issuer.condition.clientattestation;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import net.openid.conformance.condition.client.AbstractGenerateKey;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Date;

public class GenerateClientAttestationClientInstanceKey extends AbstractGenerateKey {

	@Override
	public Environment evaluate(Environment env) {

		// TODO make alg configurable
		JWK clientInstanceKey = this.createJwkForAlg("ES256");
		String clientInstanceKeyJson = clientInstanceKey.toJSONString();
		env.putString("vci", "client_attestation_key_id", clientInstanceKey.getKeyID());
		env.putString("vci", "client_attestation_key", clientInstanceKeyJson);

		String base64Cert = generateCertificateFromKey(clientInstanceKey); // TODO remove this!!

		env.putString("vci", "client_attestation_certs", base64Cert);

		log("Generated client_attestation_key", args("client_attestation_key", clientInstanceKeyJson));

		return env;
	}

	protected String generateCertificateFromKey(JWK clientInstanceKey) {

		ECPublicKey publicKey;
		ECPrivateKey privateKey;
		try {
			publicKey = clientInstanceKey.toECKey().toECPublicKey();
			privateKey = clientInstanceKey.toECKey().toECPrivateKey();
		} catch (JOSEException e) {
			throw error("Couldn't parse keys", e);
		}

		X500Name subject = new X500Name("CN=ConformanceTestWallet, OU=OpenID, O=OIDF");
		BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
		Date notBefore = new Date();
		Date notAfter = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L); // 1 year

		ContentSigner signer;
		try {
			signer = new JcaContentSignerBuilder("SHA256withECDSA").build(privateKey);
		} catch (OperatorCreationException e) {
			throw error("Couldn't create content signer", e);
		}
		X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
			subject, serial, notBefore, notAfter, subject, publicKey);

		try {
			X509Certificate cert = new JcaX509CertificateConverter()
				.setProvider(new BouncyCastleProvider())
				.getCertificate(certBuilder.build(signer));
			String base64Cert = Base64.getEncoder().encodeToString(cert.getEncoded());
			return base64Cert;
		} catch (CertificateException e) {
			throw error("Couldn't encode certificate", e);
		}
	}

	@Override
	protected JWKGenerator<? extends JWK> onConfigure(JWKGenerator<? extends JWK> generator) {
		generator.keyID("clientInstanceKey");

		return generator;
	}
}
