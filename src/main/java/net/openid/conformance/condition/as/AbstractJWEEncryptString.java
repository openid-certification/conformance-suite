package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.util.JWEUtil;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;

/**
 * Can be used to encrypt id tokens, userinfo responses, request objects
 */
public abstract class AbstractJWEEncryptString extends AbstractCondition {

	/**
	 *
	 * @param destination the entity the object is for (usually client or server)
	 * @param stringToBeEncrypted e.g the id_token as string
	 * @param clientSecret null if encryptor has no secret, e.g using private_key_jwt
	 * @param jwksJsonObject null if destination does not have a jwks
	 * @param alg
	 * @param enc
	 * @param algMetadataName used for logging only
	 * @param encMetadataName used for logging only
	 * @return
	 */
	public String encrypt(String destination, String stringToBeEncrypted, String clientSecret, JsonObject jwksJsonObject,
						  String alg, String enc, String algMetadataName, String encMetadataName) {

		if(alg == null) {
			throw error(algMetadataName + " is not defined for the " + destination + ". This is a bug in the test module. skipIfElementMissing should be used");
		}

		if(enc != null && alg == null) {
			throw error(encMetadataName + " is set but " + algMetadataName + " is not set for the " + destination + "." +
						" When " + encMetadataName + " is set, " + algMetadataName + " MUST also be provided.");
		}

		//https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
		//id_token_encrypted_response_enc
		//  OPTIONAL. JWE enc algorithm [JWA] REQUIRED for encrypting the ID Token issued to this Client.
		//  If id_token_encrypted_response_alg is specified, the default for this value is A128CBC-HS256.
		//Also for JARM:
		// ...If authorization_encrypted_response_alg is specified, the default for this value is A128CBC-HS256...
		EncryptionMethod encryptionMethod = EncryptionMethod.A128CBC_HS256;
		if(enc!=null) {
			encryptionMethod = EncryptionMethod.parse(enc);
		}
		JWEAlgorithm algorithm = JWEAlgorithm.parse(alg);

		JWK recipientJWK = null;
		if(JWEAlgorithm.Family.ASYMMETRIC.contains(algorithm)) {
			//asymmetric key
			if(jwksJsonObject==null) {
				throw error(destination + " jwks is required for " + algorithm.getName() + " algorithm");
			}
			JWKSet jwks = null;
			try {
				jwks = JWKUtil.parseJWKSet(jwksJsonObject.toString());
			} catch (ParseException e) {
				throw error("Failed to parse " + destination + " jwks", e, args("jwks", jwksJsonObject));
			}
			recipientJWK = JWEUtil.selectAsymmetricKeyForEncryption(jwks, algorithm);
			if(recipientJWK==null) {
				throw error("A key suitable for encrypting the JWT was not found in the "+destination+" JWKS.",
					args("algorithm", algorithm, "jwks", jwksJsonObject));
			}
		} else {
			//symmetric key
			try
			{
				recipientJWK = JWEUtil.createSymmetricJWKForAlgAndSecret(clientSecret, algorithm, encryptionMethod, null);
			} catch (KeyLengthException e) {
				throw error("Failed to create symmetric encryption key", e, args("algorithm", algorithm));
			}
			if(recipientJWK==null) {
				throw error("Failed to derive symmetric key", args("algorithm", algorithm));
			}
		}

		// Encrypt with the recipient's public key
		JWEEncrypter jweEncrypter = null;
		try {
			jweEncrypter = JWEUtil.createEncrypter(recipientJWK);
		} catch (JOSEException e) {
			throw error("Failed to create jwk encrypter", e);
		}

		JWEHeader.Builder jweHeaderBuilder = new JWEHeader.Builder(algorithm, encryptionMethod)
			.contentType("JWT"); // required to indicate nested JWT
		if(!Strings.isNullOrEmpty(recipientJWK.getKeyID())) {
			jweHeaderBuilder.keyID(recipientJWK.getKeyID());
		}
		JWEObject jweObject = new JWEObject(
				jweHeaderBuilder.build(),
				new Payload(stringToBeEncrypted));
		try {
			jweObject.encrypt(jweEncrypter);
		} catch (JOSEException e) {
			throw error("Encryption failed", e, args("encrypter", jweEncrypter.getClass().getSimpleName()));
		}

		// Serialise to JWE compact form
		String jweString = jweObject.serialize();

		return jweString;
	}

}
