package net.openid.conformance.condition.as;

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
import java.util.UUID;

/**
 * Can be used to encrypt id tokens, userinfo responses
 */
public abstract class AbstractJWEEncryptStringToClient extends AbstractCondition {

	/**
	 *
	 * @param stringToBeEncrypted e.g the id_token as string
	 * @param clientSecret null if client has no secret, e.g using private_key_jwt
	 * @param clientJwksJsonObject null if client does not have a jwks
	 * @param alg
	 * @param enc
	 * @param algMetadataName used for logging only
	 * @param encMetadataName used for logging only
	 * @return
	 */
	public String encrypt(String stringToBeEncrypted, String clientSecret, JsonObject clientJwksJsonObject,
						  String alg, String enc, String algMetadataName, String encMetadataName) {

		if(alg == null) {
			throw error(algMetadataName + " is not defined for the client. This is a bug in the test module. skipIfElementMissing should be used");
		}

		if(enc != null && alg == null) {
			throw error(encMetadataName + " is set but " + algMetadataName + " is not set for the client." +
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

		JWEObject jweObject = new JWEObject(
			new JWEHeader.Builder(algorithm, encryptionMethod)
				.contentType("JWT") // required to indicate nested JWT
				.build(),
			new Payload(stringToBeEncrypted));

		JWK recipientJWK = null;
		if(JWEAlgorithm.Family.ASYMMETRIC.contains(algorithm)) {
			//asymmetric key
			if(clientJwksJsonObject==null) {
				throw error("Client jwks is required for " + algorithm.getName() + " algorithm");
			}
			JWKSet clientJwks = null;
			try {
				clientJwks = JWKUtil.parseJWKSet(clientJwksJsonObject.toString());
			} catch (ParseException e) {
				throw error("Failed to parse client jwks", e, args("client_jwks", clientJwksJsonObject));
			}
			recipientJWK = JWEUtil.selectAsymmetricKeyForEncryption(clientJwks, algorithm);
		} else {
			//symmetric key
			try
			{
				recipientJWK = JWEUtil.createSymmetricJWKForAlgAndSecret(clientSecret, algorithm, encryptionMethod, UUID.randomUUID().toString());
			} catch (KeyLengthException e) {
				throw error("Failed to create symmetric encryption key", e, args("algorithm", algorithm));
			}
		}
		if(recipientJWK==null) {
			throw error("Failed to select a key", args("algorithm", algorithm));
		}
		// Encrypt with the recipient's public key
		JWEEncrypter jweEncrypter = null;
		try {
			jweEncrypter = JWEUtil.createEncrypter(recipientJWK);
		} catch (JOSEException e) {
			throw error("Failed to create jwk encrypter", e);
		}
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
