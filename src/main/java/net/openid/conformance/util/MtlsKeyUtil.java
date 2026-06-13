package net.openid.conformance.util;

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

public final class MtlsKeyUtil {

	private MtlsKeyUtil() {
	}

	public static PrivateKey generateAlgPrivateKeyFromDER(String alg, byte[] keyBytes) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		try {
			// try to generate private key using PKCS8, works for both RSA and EC and Ed25519 alg
			// RSA alg will handle both PKCS1 and PKCS8 format here
			// EC alg will throw exception for PKCS1, Ed25519 not possible with PKCS1
			KeySpec kspec = new PKCS8EncodedKeySpec(keyBytes);
			return KeyFactory.getInstance(alg, BouncyCastleProviderSingleton.getInstance()).generatePrivate(kspec);
		} catch (InvalidKeySpecException e) {
			if ("EC".equals(alg)) {
				// try to generate private key using PKCS1
				ASN1Sequence seq = ASN1Sequence.getInstance(keyBytes);
				org.bouncycastle.asn1.sec.ECPrivateKey pKey = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(seq);
				AlgorithmIdentifier algId = new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, pKey.getParametersObject());
				byte[] server_pkcs8 = new PrivateKeyInfo(algId, pKey).getEncoded();
				return KeyFactory.getInstance(alg, BouncyCastleProviderSingleton.getInstance()).generatePrivate(new PKCS8EncodedKeySpec(server_pkcs8));
			}
			throw e;
		}
	}

}
