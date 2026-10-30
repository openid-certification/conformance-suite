package net.openid.conformance.condition.common.sockets;


import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.function.Supplier;

@SuppressWarnings("ArrayRecordComponent")
public record MtlsSettings(Certificate[] clientCert, PrivateKey clientKey) {

	public static Supplier<MtlsSettings> mtlsFromPEM(
		Supplier<String> pemCertificate,
		Supplier<String> pemPrivateKey
	)  {
		return () -> {
			try {
				// 2. Parse the Certificate String

				CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

				ByteArrayInputStream certStream = new ByteArrayInputStream(pemCertificate.get().getBytes(StandardCharsets.UTF_8));
				Certificate certificate = certFactory.generateCertificate(certStream);

				// 3. Parse the Private Key String (Assumes PKCS#8 format)
				// Clean up the PEM headers/footers and whitespace
				String privateKeyRaw = pemPrivateKey.get()
					.replace("-----BEGIN PRIVATE KEY-----", "")
					.replace("-----END PRIVATE KEY-----", "")
					.replaceAll("\\s+", "");

				byte[] decodedKeyBytes = Base64.getDecoder().decode(privateKeyRaw);
				PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKeyBytes);

				// Try RSA first; adjust the algorithm string (e.g., "EC") if using Elliptic Curve
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

				// 4. Set the key entry with its accompanying certificate chain
				Certificate[] chain;
				chain = new Certificate[]{ certificate };

				return new  MtlsSettings(chain, privateKey);
			} catch (CertificateException | InvalidKeySpecException | NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		};

	}
}
