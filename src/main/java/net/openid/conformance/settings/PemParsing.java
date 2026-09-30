package net.openid.conformance.settings;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.util.encoders.DecoderException;

import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses the PEM certificates and private keys an admin pastes into the server settings page.
 * Every failure, including BouncyCastle's unchecked base64 {@link DecoderException}, is an
 * {@link IllegalArgumentException} whose message can be shown to the admin.
 */
public final class PemParsing {

	private static final SecureRandom RANDOM = new SecureRandom();

	// The JDK's EC key factory cannot convert SEC1 ("EC PRIVATE KEY") keys, so key conversion and
	// the match check use BouncyCastle directly, without relying on it being a registered provider.
	private static final BouncyCastleProvider BOUNCY_CASTLE = new BouncyCastleProvider();

	private PemParsing() {
	}

	/**
	 * @return the certificates in the order they appear, which for a chain is leaf first
	 */
	public static List<X509Certificate> parseCertificateChain(String pem) {
		if (pem == null || pem.isBlank()) {
			throw new IllegalArgumentException("no certificate found");
		}
		JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
		List<X509Certificate> chain = new ArrayList<>();
		try (PEMParser parser = new PEMParser(new StringReader(trimLines(pem)))) {
			for (Object object = parser.readObject(); object != null; object = parser.readObject()) {
				if (!(object instanceof X509CertificateHolder holder)) {
					throw new IllegalArgumentException("contains something other than a certificate");
				}
				chain.add(converter.getCertificate(holder));
			}
		} catch (IOException | CertificateException | DecoderException e) {
			throw new IllegalArgumentException("not a valid PEM certificate: " + e.getMessage(), e);
		}
		if (chain.isEmpty()) {
			throw new IllegalArgumentException("no certificate found");
		}
		return List.copyOf(chain);
	}

	/**
	 * Accepts an unencrypted PKCS#8 ("PRIVATE KEY") or PKCS#1/SEC1 ("RSA PRIVATE KEY", "EC PRIVATE KEY") key.
	 */
	public static PrivateKey parsePrivateKey(String pem) {
		if (pem == null || pem.isBlank()) {
			throw new IllegalArgumentException("no private key found");
		}
		Object object;
		try (PEMParser parser = new PEMParser(new StringReader(trimLines(pem)))) {
			object = parser.readObject();
			if (object == null) {
				throw new IllegalArgumentException("no private key found");
			}
			if (parser.readObject() != null) {
				throw new IllegalArgumentException("contains more than one PEM object; paste a single private key");
			}
		} catch (IOException | DecoderException e) {
			throw new IllegalArgumentException("not a valid PEM private key: " + e.getMessage(), e);
		}

		if (object instanceof PKCS8EncryptedPrivateKeyInfo || object instanceof PEMEncryptedKeyPair) {
			throw new IllegalArgumentException("encrypted private keys are not supported; paste the key without a passphrase");
		}
		try {
			JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BOUNCY_CASTLE);
			if (object instanceof PEMKeyPair keyPair) {
				return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
			}
			if (object instanceof PrivateKeyInfo keyInfo) {
				return converter.getPrivateKey(keyInfo);
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("not a valid PEM private key: " + e.getMessage(), e);
		}
		throw new IllegalArgumentException("not a private key");
	}

	/**
	 * Signs a random challenge with {@code key} and verifies it with the certificate's public key.
	 * A key of a different type than the certificate's does not match.
	 */
	public static boolean keyMatchesCertificate(PrivateKey key, X509Certificate certificate) {
		String algorithm = switch (key.getAlgorithm()) {
			case "RSA" -> "SHA256withRSA";
			case "EC", "ECDSA" -> "SHA256withECDSA";
			default -> throw new IllegalArgumentException("unsupported private key type " + key.getAlgorithm()
				+ "; use an RSA or EC key");
		};
		byte[] challenge = new byte[32];
		RANDOM.nextBytes(challenge);
		try {
			Signature signer = Signature.getInstance(algorithm, BOUNCY_CASTLE);
			signer.initSign(key);
			signer.update(challenge);
			byte[] signature = signer.sign();

			Signature verifier = Signature.getInstance(algorithm, BOUNCY_CASTLE);
			verifier.initVerify(certificate.getPublicKey());
			verifier.update(challenge);
			return verifier.verify(signature);
		} catch (InvalidKeyException | SignatureException e) {
			return false;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("BouncyCastle lacks " + algorithm, e);
		}
	}

	/** PEMParser only recognises boundary lines that start in the first column. */
	private static String trimLines(String pem) {
		return pem.lines().map(String::strip).collect(Collectors.joining("\n"));
	}
}
