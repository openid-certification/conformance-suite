package net.openid.conformance.settings;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.sec.ECPrivateKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keys, certificates, PEMs and JWKS for the settings unit tests.
 */
final class TestPki {

	private static final AtomicLong SERIAL = new AtomicLong(1);

	private TestPki() {
	}

	static KeyPair rsaKeyPair() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		return generator.generateKeyPair();
	}

	static KeyPair ecKeyPair() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new ECGenParameterSpec("secp256r1"));
		return generator.generateKeyPair();
	}

	/** A certificate for {@code subjectKeys}, signed by {@code issuerKeys} under {@code issuerCn}. */
	static X509Certificate certificate(KeyPair subjectKeys, String subjectCn, KeyPair issuerKeys, String issuerCn,
			Instant notBefore, Instant notAfter) throws Exception {
		X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
			new X500Name("CN=" + issuerCn),
			BigInteger.valueOf(SERIAL.getAndIncrement()),
			Date.from(notBefore),
			Date.from(notAfter),
			new X500Name("CN=" + subjectCn),
			subjectKeys.getPublic());
		String algorithm = "RSA".equals(issuerKeys.getPrivate().getAlgorithm()) ? "SHA256withRSA" : "SHA256withECDSA";
		return new JcaX509CertificateConverter()
			.getCertificate(builder.build(new JcaContentSignerBuilder(algorithm).build(issuerKeys.getPrivate())));
	}

	static X509Certificate selfSigned(KeyPair keys, String cn) throws Exception {
		Instant now = Instant.now();
		return certificate(keys, cn, keys, cn, now.minus(1, ChronoUnit.DAYS), now.plus(365, ChronoUnit.DAYS));
	}

	static X509Certificate expiredSelfSigned(KeyPair keys, String cn) throws Exception {
		Instant now = Instant.now();
		return certificate(keys, cn, keys, cn, now.minus(400, ChronoUnit.DAYS), now.minus(30, ChronoUnit.DAYS));
	}

	/** Certificates, and RSA keys in PKCS#1 ("RSA PRIVATE KEY"), as written by BouncyCastle. */
	static String pem(Object... objects) throws Exception {
		StringWriter out = new StringWriter();
		try (JcaPEMWriter writer = new JcaPEMWriter(out)) {
			for (Object object : objects) {
				writer.writeObject(object);
			}
		}
		return out.toString();
	}

	/** An EC key as OpenSSL writes it: SEC1 "EC PRIVATE KEY" carrying the named curve. */
	static String sec1Pem(PrivateKey ecKey) throws Exception {
		PrivateKeyInfo info = PrivateKeyInfo.getInstance(ecKey.getEncoded());
		ECPrivateKey inner = ECPrivateKey.getInstance(info.parsePrivateKey());
		ECPrivateKey withCurve = new ECPrivateKey(256, inner.getKey(), info.getPrivateKeyAlgorithm().getParameters());
		return pem(new PemObject("EC PRIVATE KEY", withCurve.getEncoded()));
	}

	static String pkcs8Pem(PrivateKey key) throws Exception {
		return pem(new JcaPKCS8Generator(key, null));
	}

	static String encryptedPkcs8Pem(PrivateKey key) throws Exception {
		OutputEncryptor encryptor = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
			.setProvider(new BouncyCastleProvider())
			.setPassword("password".toCharArray())
			.build();
		return pem(new JcaPKCS8Generator(key, encryptor));
	}

	static RSAKey rsaJwk(String kid) throws Exception {
		return new RSAKeyGenerator(2048)
			.keyID(kid)
			.algorithm(JWSAlgorithm.PS256)
			.keyUse(KeyUse.SIGNATURE)
			.generate();
	}

	/** A JWKS string including the private members. */
	static String privateJwks(RSAKey key) {
		return new JWKSet(key).toString(false);
	}
}
