package net.openid.conformance.util;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.AESDecrypter;
import com.nimbusds.jose.crypto.AESEncrypter;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JWEUtil {

	private static Logger logger = LoggerFactory.getLogger(JWEUtil.class);
	/**
	 * returns a key that has the correct key type and optionally use=enc
	 * or null if no key was found
	 * Only for RSA or EC keys
	 * @param jwkSet
	 * @param alg
	 * @return
	 */
	public static JWK selectAsymmetricKeyForEncryption(JWKSet jwkSet, JWEAlgorithm alg) {
		if(jwkSet==null) {
			return null;
		}

		KeyType keyType = null;
		if(JWEAlgorithm.Family.RSA.contains(alg)) {
			keyType = KeyType.RSA;
		} else if(JWEAlgorithm.Family.ECDH_ES.contains(alg)) {
			keyType = KeyType.EC;
		}

		JWKMatcher jwkMatcher = new JWKMatcher.Builder().keyType(keyType).build();
		JWK currentMatch = null;
		for(JWK jwk : jwkSet.getKeys()) {
			if(jwkMatcher.matches(jwk)) {
				if(currentMatch==null) {
					currentMatch = jwk;
				} else {
					if(!KeyUse.ENCRYPTION.equals(currentMatch.getKeyUse()) && KeyUse.ENCRYPTION.equals(jwk.getKeyUse())) {
						//this is a better match
						currentMatch = jwk;
					}
				}
			}
		}
		return currentMatch;
	}

	/**
	 * https://openid.net/specs/openid-connect-core-1_0.html#Encryption
	 * The symmetric encryption key is derived from the client_secret value by using a left truncated SHA-2
	 * hash of the octets of the UTF-8 representation of the client_secret.
	 * For keys of 256 or fewer bits, SHA-256 is used; for keys of 257-384 bits, SHA-384 is used;
	 * for keys of 385-512 bits, SHA-512 is used. The hash value MUST be left truncated to the appropriate
	 * bit length for the AES key wrapping or direct encryption algorithm used, for instance,
	 * truncating the SHA-256 hash to 128 bits for A128KW.
	 *
	 * @param algorithm
	 * @param inputString
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] deriveEncryptionKey(String algorithm, String inputString)
	{
		MessageDigest digester;
		int targetLength = 16;
		String digestAlgorithm = "SHA-256";

		//regexes and logic from Filip's openid-client. "derivedKey(len) in client.js"
		Matcher matcher = Pattern.compile("^A(\\d{3})(GCM)?KW$").matcher(algorithm);
		String matchedNumber = null;
		if (matcher.matches()) {
			matchedNumber = matcher.group(1);
		} else {
			matcher = Pattern.compile("^A(\\d{3})(GCM|CBC-HS(\\d{3}))").matcher(algorithm);
			if(matcher.matches()) {
				matchedNumber = matcher.group(3);
			}
		}

		switch (matchedNumber) {
			case "128":
				targetLength = 16;
				break;
			case "192":
				targetLength = 24;
				break;
			case "256":
				targetLength = 32;
				break;
			default:
				//TODO handle this better?
				return null;
		}

		try {
			digester = MessageDigest.getInstance(digestAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			//TODO throw this error? should not happen in practice. possible only if sha256 is not available
			return null;
		}

		byte[] digest = digester.digest(inputString.getBytes(StandardCharsets.UTF_8));

		byte[] keyBytes = new byte[targetLength];
		System.arraycopy(digest, 0, keyBytes, 0, targetLength);

		if(logger.isDebugEnabled()) {
			logger.debug("Derived Key:" + Base64URL.encode(keyBytes).toJSONString());
		}

		return keyBytes;
	}

	/**
	 * AES or "dir" only
	 * @param secret
	 * @param algorithm
	 * @param encMethod
	 * @param keyId
	 * @return
	 * @throws KeyLengthException
	 */
	public static JWK createSymmetricJWKForAlgAndSecret(String secret, JWEAlgorithm algorithm, EncryptionMethod encMethod, String keyId) throws KeyLengthException {
		OctetSequenceKey key = null;
		if(JWEAlgorithm.Family.AES_GCM_KW.contains(algorithm) || JWEAlgorithm.Family.AES_KW.contains(algorithm)) {
			byte[] secretBytes = deriveEncryptionKey(algorithm.getName(), secret);
			key = new OctetSequenceKey.Builder(secretBytes).
				keyUse(KeyUse.ENCRYPTION).keyID(keyId).algorithm(algorithm).
				build();
		} else if(JWEAlgorithm.DIR.equals(algorithm)) {
			byte[] secretBytes = deriveEncryptionKey(encMethod.getName(), secret);
			key = new OctetSequenceKey.Builder(secretBytes).
				keyUse(KeyUse.ENCRYPTION).keyID(keyId).algorithm(algorithm).
				build();
		}
		return key;
	}

	/**
	 * may return null when it doesn't know how to handle the key
	 * @param key
	 * @return AESEncrypter or DirectEncrypter or RSAEncrypter or ECDHEncrypter
	 * @throws JOSEException
	 */
	public static JWEEncrypter createEncrypter(JWK key) throws JOSEException
	{
		if(key==null) {
			return null;
		}
		if(KeyType.OCT.equals(key.getKeyType())) {
			if(AESEncrypter.SUPPORTED_ALGORITHMS.contains(key.getAlgorithm()) ) {
				AESEncrypter encrypter = new AESEncrypter((OctetSequenceKey)key);
				return encrypter;
			} else if(DirectEncrypter.SUPPORTED_ALGORITHMS.contains(key.getAlgorithm())) {
				DirectEncrypter directEncrypter = new DirectEncrypter((OctetSequenceKey)key);
				return directEncrypter;
			}
		} else if(KeyType.RSA.equals(key.getKeyType())) {
			RSAEncrypter rsaEncrypter = new RSAEncrypter((RSAKey)key);
			return rsaEncrypter;
		} else if(KeyType.EC.equals(key.getKeyType())) {
			ECDHEncrypter ecdhEncrypter = new ECDHEncrypter((ECKey)key);
			return ecdhEncrypter;
		} else {
			//TODO what to do in this case?
			return null;
		}
		return null;
	}

	/**
	 * @param key
	 * @return AESDecrypter or DirectDecrypter or RSADecrypter or ECDHDecrypter or null
	 * @throws JOSEException
	 */
	public static JWEDecrypter createDecrypter(JWK key) throws JOSEException
	{
		if(AESDecrypter.SUPPORTED_ALGORITHMS.contains(key.getAlgorithm())) {
			AESDecrypter decrypter = new AESDecrypter((OctetSequenceKey)key);
			return decrypter;
		} else if(DirectDecrypter.SUPPORTED_ALGORITHMS.contains(key.getAlgorithm())) {
			DirectDecrypter directDecrypter = new DirectDecrypter((OctetSequenceKey)key);
			return directDecrypter;
		} else if(RSADecrypter.SUPPORTED_ALGORITHMS.contains(key.getAlgorithm())) {
			RSADecrypter rsaDecrypter = new RSADecrypter((RSAKey)key);
			return rsaDecrypter;
		} else if(ECDHDecrypter.SUPPORTED_ALGORITHMS.contains(key.getAlgorithm())) {
			ECDHDecrypter ecdhDecrypter = new ECDHDecrypter((ECKey)key);
			return ecdhDecrypter;
		} else {
			//TODO what to do in this case?
			return null;
		}
	}
}
