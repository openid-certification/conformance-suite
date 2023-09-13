package net.openid.conformance.util;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObject;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Pattern;

public class JWTUtil {

	/**
	 * wrapper for Nimbus JWTParser
	 * just in case we want to override something one day
	 * @param jwtAsString
	 * @return
	 * @throws ParseException
	 */
	public static JWT parseJWT(String jwtAsString) throws ParseException {
		validateJwtContainsOnlyAllowedCharacters(jwtAsString);
		JWT jwt = JWTParser.parse(jwtAsString);
		return jwt;
	}

	public static void validateJwtContainsOnlyAllowedCharacters(String jwt) throws ParseException {
		// the allowed characters is base64url plus '.'
		String regex = "[.a-zA-Z0-9_-]";
		for (char character : jwt.toCharArray()) {
			if (!Pattern.matches(regex, String.valueOf(character))) {
				throw new ParseException(String.format("The jwt is invalid because it contains the character %s that is neither a '.' nor one permitted in unpadded base64url", character), 0);
			}
		}

	}

	/**
	 * Also see jwtStringToJsonObjectForEnvironment
	 * @param jwt
	 * @return
	 * @throws ParseException
	 */
	public static JsonObject jwtClaimsSetAsJsonObject(JWT jwt) throws ParseException {
		//added this variable to make it obvious
		boolean includeNullValues = true;
		JWTClaimsSet jwtClaimsSet;
		if (!(jwt instanceof JOSEObject)) {
			throw new RuntimeException("jwt is not an instance of JOSEObject");
		}

		// This code does multiple conversions to JSON; we could just call 'JsonParser.parseString' here, however
		// that seems to result in the unit test failing as we no longer detect JSON payloads that have the same
		// claim more than once.
		String jsonPayload = ((JOSEObject)jwt).getPayload().toString();
		if (jsonPayload == null) {
			throw new ParseException("Failed to get JWT payload as a string", 0);
		}

		jwtClaimsSet = JWTClaimsSet.parse(JSONObjectUtils.parse(jsonPayload));
		if (jwtClaimsSet == null) {
			throw new ParseException("Failed to extract JWT claims", 0);
		}
		JsonObject claims = JsonParser.parseString(JSONObjectUtils.toJSONString(jwtClaimsSet.toJSONObject(includeNullValues))).getAsJsonObject();
		return claims;
	}

	/**
	 * Also see jwtStringToJsonObjectForEnvironment
	 * Note: Nimbusds will always remove null values from JWT headers
	 * @param jwt
	 * @return
	 */
	public static JsonObject jwtHeaderAsJsonObject(JWT jwt) {
		JsonObject header = JsonParser.parseString(JSONObjectUtils.toJSONString(jwt.getHeader().toJSONObject())).getAsJsonObject();
		return header;
	}

	/**
	 * Parses the JWT and returns a JsonObject with value, header and claims entries
	 * @param jwtAsString
	 * @return
	 * @throws ParseException
	 */
	public static JsonObject jwtStringToJsonObjectForEnvironment(String jwtAsString) throws ParseException {
		JWT token = JWTUtil.parseJWT(jwtAsString);

		if(token instanceof EncryptedJWT) {
			throw new ParseException("EncryptedJWT found, which this test currently doesn't support", 0);
		}
		JsonObject header = JWTUtil.jwtHeaderAsJsonObject(token);
		JsonObject claims = JWTUtil.jwtClaimsSetAsJsonObject(token);

		return createJsonObjectForEnvironment(jwtAsString, header, claims);
	}

	private static JsonObject createJsonObjectForEnvironment(String jwtString, JsonObject header, JsonObject claims) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("value", jwtString);
		jsonObject.add("header", header);
		jsonObject.add("claims", claims);
		return jsonObject;
	}

	/**
	 * use if the JWT may be encrypted, e.g an encrypted request object
	 *
	 * @param client Client object containing client_secret, required if symmetric encryption used
	 * @param privateJwksWithEncKeys Key for decryption, if encryption used
	 * @return may return null if decryption fails
	 */
	public static JsonObject jwtStringToJsonObjectForEnvironment(String jwtAsString, JsonObject client, JsonObject privateJwksWithEncKeys)
		throws ParseException, JOSEException {
		JWT token = JWTUtil.parseJWT(jwtAsString);
		if(token instanceof EncryptedJWT) {
			EncryptedJWT encryptedJWT = (EncryptedJWT) token;
			JsonObject jweHeader = JWTUtil.jwtHeaderAsJsonObject(token);
			JWEAlgorithm alg = encryptedJWT.getHeader().getAlgorithm();
			JWK decryptionKey = null;
			if(JWEAlgorithm.Family.SYMMETRIC.contains(alg)) {
				if (client == null) {
					throw new ParseException("A client secret is required to decrypt this JWT", 0);
				}
				String client_secret = OIDFJSON.getString(client.get("client_secret"));
				if (client_secret == null) {
					throw new ParseException("A client secret is required to decrypt this JWT", 0);
				}
				decryptionKey = JWEUtil.createSymmetricJWKForAlgAndSecret(client_secret,
										alg, encryptedJWT.getHeader().getEncryptionMethod(), null);
			} else {
				if (privateJwksWithEncKeys == null) {
					throw new ParseException("A JWKS is required to decrypt this JWT", 0);
				}
				JWKSet jwkSet = JWKUtil.parseJWKSet(privateJwksWithEncKeys.toString());
				decryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, alg);
				if (decryptionKey == null) {
					throw new ParseException("No suitable key for decrypting this JWT was provided in the test configuration.", 0);
				}
			}
			JWEDecrypter decrypter = JWEUtil.createDecrypter(alg, decryptionKey);
			encryptedJWT.decrypt(decrypter);
			String decryptedJwt = encryptedJWT.getPayload().toString();
			try {
				validateJwtContainsOnlyAllowedCharacters(decryptedJwt);
			} catch (ParseException e) {
				throw new ParseException("The JWE has been decrypted. " + e.getMessage(), 0);
			}
			JsonObject out = JWTUtil.jwtStringToJsonObjectForEnvironment(decryptedJwt);
			out.add("jwe_header", jweHeader);
			return out;
		} else {
			JsonObject header = JWTUtil.jwtHeaderAsJsonObject(token);
			JsonObject claims = JWTUtil.jwtClaimsSetAsJsonObject(token);
			return createJsonObjectForEnvironment(jwtAsString, header, claims);
		}
	}

	public static String generateDocuSignJWTAssertion(
		byte[] rsaPrivateKey, String aud, String iss, String userId, long expiresIn, String scopes)  throws Exception {

		if (expiresIn <= 0L) {
			throw new IllegalArgumentException("expiresIn should be a non-negative value");
		}
		if (rsaPrivateKey == null || rsaPrivateKey.length == 0) {
			throw new IllegalArgumentException("rsaPrivateKey byte array is empty");
		}
		if (Strings.isNullOrEmpty(aud) || Strings.isNullOrEmpty(iss)) {
			throw new IllegalArgumentException("One of aud or iss is null or empty");
		}

		RSAPrivateKey privateKey = readRSAPrivateKeyFromByteArray(rsaPrivateKey);
		long now = System.currentTimeMillis();

		// Create JWT claims
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
			.issuer(iss)
			.audience(aud)
			.subject(userId)
			.issueTime(new Date(now))
			.claim("scope", scopes)
			.expirationTime(new Date(System.currentTimeMillis() + expiresIn * 1000))
			.build();
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
		SignedJWT signedJWT = new SignedJWT(header, claimsSet);
		JWSSigner signer = new RSASSASigner(privateKey);
		signedJWT.sign(signer);
		return signedJWT.serialize();
	}

	private static RSAPrivateKey readRSAPrivateKeyFromByteArray(byte[] privateKeyBytes)
		throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		try (PemReader reader = new PemReader(new StringReader(new String(privateKeyBytes)))) {
			PemObject pemObject = reader.readPemObject();
			byte[] bytes = pemObject.getContent();
			RSAPrivateKey privateKey = null;
			KeyFactory kf = KeyFactory.getInstance("RSA", "BC");
			EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
			privateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
			return privateKey;
		}
	}
}
