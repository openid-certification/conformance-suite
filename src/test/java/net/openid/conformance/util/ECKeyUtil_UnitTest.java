package net.openid.conformance.util;

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ECKeyUtil_UnitTest {

	@Test
	void derivedPublicKey_matches_publicKey() {

		assertDoesNotThrow(() -> {
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("EC", BouncyCastleProviderSingleton.getInstance());
			kpGen.initialize(new ECGenParameterSpec("P-256"));
			KeyPair keyPair = kpGen.generateKeyPair();

			PrivateKey privateKey = keyPair.getPrivate();
			PublicKey publicKey = keyPair.getPublic();

			if((privateKey instanceof BCECPrivateKey) && (publicKey instanceof BCECPublicKey) ) {
				BCECPrivateKey bceCPrivateKey = (BCECPrivateKey) privateKey;
				BCECPublicKey bceCPublicKey = (BCECPublicKey) publicKey;
				BCECPublicKey derivedPubKey = ECKeyUtil.deriveECPubKeyFromPrivKey(bceCPrivateKey);
				assertTrue(bceCPublicKey.equals(derivedPubKey));
			} else {
				throw new ClassCastException("Invalid EC key instance");
			}
		});
	}

	@Test
	void derivedPublicKey_using_publickey_matches() {

		assertDoesNotThrow(() -> {
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("EC", BouncyCastleProviderSingleton.getInstance());
			kpGen.initialize(new ECGenParameterSpec("P-256"));
			KeyPair keyPair = kpGen.generateKeyPair();

			PrivateKey privateKey = keyPair.getPrivate();
			PublicKey publicKey = keyPair.getPublic();

			PublicKey derivedPubKey = ECKeyUtil.deriveECPubKeyFromPrivKey((BCECPrivateKey) privateKey);
			assertTrue(publicKey.equals(derivedPubKey));
		});
	}

	@Test
	void multiple_derivedPublicKey_matches_publicKey() {

		assertDoesNotThrow(() -> {
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("EC", BouncyCastleProviderSingleton.getInstance());
			kpGen.initialize(new ECGenParameterSpec("P-256"));
			KeyPair keyPair = kpGen.generateKeyPair();

			PrivateKey privateKey = keyPair.getPrivate();
			PublicKey publicKey = keyPair.getPublic();

			if((privateKey instanceof BCECPrivateKey) && (publicKey instanceof BCECPublicKey) ) {
				BCECPrivateKey bceCPrivateKey = (BCECPrivateKey) privateKey;
				BCECPublicKey bceCPublicKey = (BCECPublicKey) publicKey;
				BCECPublicKey derivedPubKey1 = ECKeyUtil.deriveECPubKeyFromPrivKey(bceCPrivateKey);
				BCECPublicKey derivedPubKey2 = ECKeyUtil.deriveECPubKeyFromPrivKey(bceCPrivateKey);
				assertTrue(bceCPublicKey.equals(derivedPubKey1));
				assertTrue(bceCPublicKey.equals(derivedPubKey2));
				assertTrue(derivedPubKey1.equals(derivedPubKey2));
			} else {
				throw new ClassCastException("Invalid EC key instance");
			}
		});
	}

	@Test
	void derivedPublicKeys_fromDifferentKeyPairs_differ() {

		assertDoesNotThrow(()-> {
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("EC", BouncyCastleProviderSingleton.getInstance());
			kpGen.initialize(new ECGenParameterSpec("P-256"));
			KeyPair keyPair1 = kpGen.generateKeyPair();
			KeyPair keyPair2 = kpGen.generateKeyPair();

			PrivateKey privateKey1 = keyPair1.getPrivate();
			PublicKey publicKey1 = keyPair1.getPublic();

			PrivateKey privateKey2 = keyPair2.getPrivate();
			PublicKey publicKey2 = keyPair2.getPublic();

			if((privateKey1 instanceof BCECPrivateKey) && (publicKey1 instanceof BCECPublicKey) &&
				(privateKey2 instanceof BCECPrivateKey) && (publicKey2 instanceof BCECPublicKey) ) {
				BCECPrivateKey bceCPrivateKey1 = (BCECPrivateKey) privateKey1;
				BCECPublicKey bceCPublicKey1 = (BCECPublicKey) publicKey1;

				BCECPrivateKey bceCPrivateKey2 = (BCECPrivateKey) privateKey2;
				BCECPublicKey bceCPublicKey2 = (BCECPublicKey) publicKey2;

				BCECPublicKey derivedPubKey1 = ECKeyUtil.deriveECPubKeyFromPrivKey(bceCPrivateKey1);
				BCECPublicKey derivedPubKey2 = ECKeyUtil.deriveECPubKeyFromPrivKey(bceCPrivateKey2);

				assertFalse(derivedPubKey1.equals(bceCPublicKey2));
				assertFalse(derivedPubKey2.equals(bceCPublicKey1));

				assertFalse(derivedPubKey1.equals(derivedPubKey2));
			} else {
				throw new RuntimeException("Invalid EC key instance");
			}
		});
	}


	@Test
	void derivedPublicKeys_fromDifferentCurves_differ() {

		assertDoesNotThrow(()-> {
			KeyPairGenerator kpGen = KeyPairGenerator.getInstance("EC", BouncyCastleProviderSingleton.getInstance());
			kpGen.initialize(new ECGenParameterSpec("P-256"));
			KeyPair keyPair1 = kpGen.generateKeyPair();
			kpGen.initialize(new ECGenParameterSpec("P-384"));
			KeyPair keyPair2 = kpGen.generateKeyPair();

			PrivateKey privateKey1 = keyPair1.getPrivate();
			PublicKey publicKey1 = keyPair1.getPublic();

			PrivateKey privateKey2 = keyPair2.getPrivate();
			PublicKey publicKey2 = keyPair2.getPublic();

			if((privateKey1 instanceof BCECPrivateKey) && (publicKey1 instanceof BCECPublicKey) &&
				(privateKey2 instanceof BCECPrivateKey) && (publicKey2 instanceof BCECPublicKey) ) {
				BCECPrivateKey bceCPrivateKey1 = (BCECPrivateKey) privateKey1;
				BCECPublicKey bceCPublicKey1 = (BCECPublicKey) publicKey1;

				BCECPrivateKey bceCPrivateKey2 = (BCECPrivateKey) privateKey2;
				BCECPublicKey bceCPublicKey2 = (BCECPublicKey) publicKey2;

				BCECPublicKey derivedPubKey1 = ECKeyUtil.deriveECPubKeyFromPrivKey(bceCPrivateKey1);
				BCECPublicKey derivedPubKey2 = ECKeyUtil.deriveECPubKeyFromPrivKey(bceCPrivateKey2);

				assertFalse(derivedPubKey1.equals(bceCPublicKey2));
				assertFalse(derivedPubKey2.equals(bceCPublicKey1));

				assertFalse(derivedPubKey1.equals(derivedPubKey2));
			} else {
				throw new RuntimeException("Invalid EC key instance");
			}
		});
	}

}
