package net.openid.conformance.util;


import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class ECKeyUtil {

	private ECKeyUtil() {}

	public static BCECPublicKey deriveECPubKeyFromPrivKey(BCECPrivateKey bcecPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
		// Get EC parameters and the base point G
		BigInteger d = bcecPrivateKey.getD();
		org.bouncycastle.jce.spec.ECParameterSpec ecSpec = bcecPrivateKey.getParameters();

		// Multiply base point G by the private scalar 'd' to get public point Q
		org.bouncycastle.math.ec.ECPoint Q = ecSpec.getG().multiply(d).normalize();

		// Create the Public Key Specification
		org.bouncycastle.jce.spec.ECPublicKeySpec pubSpec = new org.bouncycastle.jce.spec.ECPublicKeySpec(Q, ecSpec);

		// Generate the ECPublicKey using Bouncy Castle's KeyFactory
		KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProviderSingleton.getInstance());
		BCECPublicKey pubKey = (BCECPublicKey) keyFactory.generatePublic(pubSpec);

		return pubKey;
	}

}
